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

import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object EncounteredMethodsEntry extends TraceFileEntry[String] {
	val path = "encountered-methods.txt"

	def reader: TraceFileEntryReader[String] = new Reader
	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[String] =
		new Writer(builder.vend(path))

	/** This string must appear at the beginning of a file
	  * that can be parsed by this object.
	  */
	private val magicString = "// Trace Data: encountered method names"

	private class Reader extends TraceFileEntryReader[String] {
		def read(content: InputStream)(callback: String => Unit): Unit = {
			val source = Source fromInputStream content
			try {
				val lines = source.getLines

				if (lines.hasNext) {
					val firstLine = lines.next
					if (firstLine == magicString) {
						lines foreach callback
					} else {
						throw new IOException(
							"Unexpected input: content does not appear to contain encountered method signatures")
					}
				} else {
					throw new IOException("The given content appears to be empty")
				}

			} finally {
				source.close
				content.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[String] {
		private val outstr = token.resource.openOutput
		private val out = new PrintStream(outstr)

		private def tryWrite(line: String): Unit = {
			try {
				out.println(line)
			} catch {
				// if an exception is caught, close the streams and re-throw it
				case e: IOException =>
					out.close
					outstr.close
					throw e
			}
		}

		//first line must be the `magicString`
		tryWrite(magicString)

		def write(item: String): Unit = tryWrite(item)

		def finish(): Unit = {
			out.close
			outstr.close
			token.completionCallback()
		}
	}
}