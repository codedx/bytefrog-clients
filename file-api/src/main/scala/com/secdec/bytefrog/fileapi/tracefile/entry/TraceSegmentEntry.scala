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
import scala.io.Source

import com.secdec.bytefrog.fileapi.data.TraceSegment
import com.secdec.bytefrog.fileapi.data.TraceSegmentJson
import com.secdec.bytefrog.fileapi.io.IOUtils._
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object TraceSegmentEntry extends TraceFileEntry[TraceSegment] {

	val path = "traces.json"

	def reader: TraceFileEntryReader[TraceSegment] = new Reader

	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[TraceSegment] =
		new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[TraceSegment] {
		def read(content: InputStream)(callback: TraceSegment => Unit): Unit = {
			try {
				val s = Source.fromInputStream(content).mkString
				val parsed = TraceSegmentJson.deserialize(s)
				for (seg <- parsed) callback(seg)
			} catch {
				case e: IOException => //noop
			} finally {
				content.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[TraceSegment] {
		private val segments = collection.mutable.ListBuffer[TraceSegment]()

		def write(seg: TraceSegment): Unit = {
			segments += seg
		}
		def finish(): Unit = {
			val s = TraceSegmentJson.serialize(segments.result)
			token.resource.openOutput closeAfter { out =>
				val ps = new PrintStream(out)
				ps.print(s)
			}
			token.completionCallback()
		}
	}
}

