/*
 * bytefrog: a tracing framework for the JVM. For more information
 * see http://code-pulse.com/bytefrog
 *
 * Copyright (C) 2014 Applied Visions - http://securedecisions.avi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.secdec.bytefrog.fileapi.tracefile.entry

import java.io.IOException
import java.io.InputStream
import java.io.PrintStream

import scala.collection.Map
import scala.io.Source

import com.secdec.bytefrog.fileapi.data.ExceptionIdMapJson
import com.secdec.bytefrog.fileapi.io.IOUtils.closeableWithCloseAfter
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object ExceptionIdEntry extends TraceFileEntry[Map[Int, String]] {

	val path = "exception-ids.json"

	def reader: TraceFileEntryReader[Map[Int, String]] = new Reader

	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[Map[Int, String]] =
		new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[Map[Int, String]] {
		def read(content: InputStream)(callback: Map[Int, String] => Unit): Unit = {
			try {
				val s = Source.fromInputStream(content).mkString
				val parsed = ExceptionIdMapJson.deserialize(s)
				callback(parsed)
			} catch {
				case e: IOException => //noop
			} finally {
				content.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[Map[Int, String]] {
		private val exceptionIds = collection.mutable.Map[Int, String]()

		def write(ids: Map[Int, String]): Unit = {
			exceptionIds ++= ids
		}

		def finish(): Unit = {
			val s = ExceptionIdMapJson.serialize(exceptionIds)
			token.resource.openOutput closeAfter { out =>
				val ps = new PrintStream(out)
				ps.print(s)
			}
			token.completionCallback()
		}
	}
}

