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

package com.secdec.bytefrog.clients.common.data.processing.processors

import scala.collection.mutable

import com.secdec.bytefrog.hq.data.processing.DataProcessor
import com.secdec.bytefrog.hq.protocol.DataMessageContent
import com.secdec.bytefrog.fileapi.tracefile.TraceFile
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

object MethodIdProcessor {
	def apply(output: TraceFileBuilder) = new MethodIdProcessor(output)
}

/** The MethodIdProcessor maintains a mapping of method IDs to their full signatures.
  *
  * @author robertf
  */
class MethodIdProcessor(output: TraceFileBuilder) extends DataProcessor {
	private val methods = mutable.Map[Int, String]()

	def processMessage(message: DataMessageContent) = message match {
		case DataMessageContent.MapMethodSignature(sig, id) =>
			methods.put(id, sig)

		case _ =>
	}

	def processDataBreak {
		// we don't care :)
	}

	def finishProcessing() {
		val writer = TraceFile.methodIdEntry.writer(output)
		writer write methods
		writer.finish
	}

	def cleanup() {
	}
}