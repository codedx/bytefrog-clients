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

import scala.collection.mutable.ListBuffer
import scala.io.Source

import com.secdec.bytefrog.fileapi.data.TraceMarker
import com.secdec.bytefrog.fileapi.data.TraceMarkerJson
import com.secdec.bytefrog.fileapi.io.IOUtils.closeableWithCloseAfter
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object MarkersEntry extends TraceFileEntry[TraceMarker] {

	val path = "markers.json"

	def reader: TraceFileEntryReader[TraceMarker] = new Reader

	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[TraceMarker] =
		new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[TraceMarker] {
		def read(content: InputStream)(callback: TraceMarker => Unit): Unit = {
			try {
				val s = Source.fromInputStream(content).mkString
				val parsed = TraceMarkerJson.deserialize(s)
				parsed foreach callback
			} catch {
				case e: IOException => //noop
			} finally {
				content.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[TraceMarker] {
		private val markers = new ListBuffer[TraceMarker]

		def write(marker: TraceMarker): Unit = {
			markers += marker
		}

		def finish(): Unit = {
			val s = TraceMarkerJson.serialize(markers.result)
			markers.clear
			token.resource.openOutput closeAfter { out =>
				val ps = new PrintStream(out)
				ps.print(s)
			}
			token.completionCallback()
		}
	}
}