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

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

import com.secdec.bytefrog.fileapi.data.MethodBucketRecord
import com.secdec.bytefrog.fileapi.data.MethodTimeBucket
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object MethodCallsEntry extends TraceFileEntry[MethodTimeBucket] {

	val path = "method-calls.bin"
	def reader: TraceFileEntryReader[MethodTimeBucket] = new Reader
	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[MethodTimeBucket] =
		new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[MethodTimeBucket] {
		def read(content: InputStream)(callback: MethodTimeBucket => Unit): Unit = {
			val dis = new DataInputStream(content)
			try {
				def readCallBuckets: List[MethodBucketRecord] = {
					val list = new ListBuffer[MethodBucketRecord]
					val count = dis.readInt

					for (i <- 0 until count) {
						list += MethodBucketRecord(dis.readInt, dis.readLong)
					}

					list.result
				}

				// "recursive" read loop. @tailrec turns it into a while loop
				@tailrec def readOne: Unit = {
					var read = false

					try {
						val startTime = dis.readInt
						read = true
						val endTime = dis.readInt
						val methods = readCallBuckets

						callback(MethodTimeBucket(startTime, endTime, methods))
					} catch {
						case e: EOFException => if (read) throw e
					}

					if (read)
						readOne
				}

				readOne
			} finally {
				dis.close
			}
		}
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[MethodTimeBucket] {

		private val out = token.resource.openOutput
		private val bout = new BufferedOutputStream(out)
		private val dout = new DataOutputStream(bout)

		def writeCallBuckets(buckets: List[MethodBucketRecord]) {
			dout writeInt buckets.length

			for (bucket <- buckets) {
				dout writeInt bucket.methodId
				dout writeLong bucket.numberCalls
			}
		}

		def write(seg: MethodTimeBucket): Unit = {
			dout writeInt seg.startTime
			dout writeInt seg.endTime
			writeCallBuckets(seg.methods)
		}

		def finish(): Unit = {
			dout.close
			bout.close
			out.close
			token.completionCallback()
		}
	}
}

