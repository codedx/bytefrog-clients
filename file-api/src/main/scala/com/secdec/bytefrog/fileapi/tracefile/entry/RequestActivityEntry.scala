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

import java.io.InputStream
import java.io.PrintWriter

import com.secdec.bytefrog.fileapi.data.RequestActivity
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilderToken
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object RequestActivityEntry extends TraceFileEntry[RequestActivity] {

	val path = "requestActivity.txt"

	def reader: TraceFileEntryReader[RequestActivity] = new Reader
	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[RequestActivity] = new Writer(builder.vend(path))

	private class Reader extends TraceFileEntryReader[RequestActivity] {
		def read(content: InputStream)(callback: RequestActivity => Unit): Unit = ???
	}

	private class Writer(token: ZipFileBuilderToken) extends TraceFileEntryWriter[RequestActivity] {
		val ostr = token.resource.openOutput
		val out = new PrintWriter(ostr)

		def write(activity: RequestActivity): Unit = {
			out.println(activity.requestId)
			for (m <- activity.activity) {
				out.println(s"\tsig ${m.sigId} - thread ${m.threadId} - ${m.sig}")
			}
		}

		def finish(): Unit = {
			out.close
			ostr.close
			token.completionCallback()
		}
	}
}