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

import java.io._
import scala.annotation.tailrec

import com.secdec.bytefrog.fileapi.data.MethodEncounters
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object MethodEncountersEntry extends TraceFileEntry[MethodEncounters] {

	def path = "method-encounters.bin"
	def reader: TraceFileEntryReader[MethodEncounters] = new Reader
	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[MethodEncounters] =
		new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[MethodEncounters] {
		def read(content: InputStream)(callback: MethodEncounters => Unit): Unit = {
			val bis = new BufferedInputStream(content)
			val dis = new DataInputStream(bis)
			try {

				// "recursive" read loop. @tailrec turns it into a while loop
				@tailrec def readOne: Unit = {
					// signal byte: `true` = keep reading, `false` = stop
					val continue = dis.readBoolean

					if (continue) {
						// format = [method sig UTF][array length = N][N x timestamps]
						val sig = dis.readUTF
						val arrayLen = dis.readInt
						val array = new Array[Int](arrayLen)
						for (i <- 0 until arrayLen) {
							array(i) = dis.readInt
						}
						callback(MethodEncounters(sig, array))
						readOne
					}
				}

				readOne

			} finally {
				dis.close
				bis.close
				content.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[MethodEncounters] {

		private val out = token.resource.openOutput
		private val bout = new BufferedOutputStream(out)
		private val dout = new DataOutputStream(bout)

		def write(item: MethodEncounters): Unit = {
			// write the "continue" signal byte
			dout.writeBoolean(true)

			// format = [method sig UTF][array length = N][N x timestamps]
			dout.writeUTF(item.methodSig)
			dout.writeInt(item.encounterTimes.size)
			for (t <- item.encounterTimes)
				dout.writeInt(t)
		}

		def finish(): Unit = {
			// write the "end" signal byte
			dout.writeBoolean(false)

			// close all of the streams
			dout.flush
			dout.close
			bout.close
			out.close

			token.completionCallback()
		}
	}

}