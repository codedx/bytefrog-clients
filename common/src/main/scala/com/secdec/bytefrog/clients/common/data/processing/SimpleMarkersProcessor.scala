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

import scala.collection.mutable.ListBuffer
import collection.mutable.{ Set => MutableSet }

import com.secdec.bytefrog.fileapi.tracefile.TraceFile
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder
import com.secdec.bytefrog.fileapi.data.TraceMarker
import com.secdec.bytefrog.fileapi.data.MethodActivity
import com.secdec.bytefrog.fileapi.data.RequestActivity
import com.secdec.bytefrog.hq.data.processing.DataProcessor
import com.secdec.bytefrog.hq.protocol.DataMessageContent

class SimpleMarkersProcessor(output: TraceFileBuilder) extends DataProcessor {
	private val writer = TraceFile.requestActivityEntry.writer(output)

	private val activities = collection.mutable.Map[String, (MutableSet[Int], ListBuffer[MethodActivity])]()
	private val methodIds = collection.mutable.Map[Int, String]()

	def processMessage(msg: DataMessageContent) = msg match {
		case DataMessageContent.Marker(key, "S", timestamp) if key.startsWith("SpyFilter-Request-") => startRequest(key)
		case DataMessageContent.Marker(key, "E", timestamp) if key.startsWith("SpyFilter-Request-") => endRequest(key)
		case DataMessageContent.MapMethodSignature(sig, id) => methodIds += (id -> sig)
		case DataMessageContent.MethodEntry(sigId, _, threadId) => methodEntered(sigId, threadId)
		case _ => //noop
	}

	private def startRequest(req: String) = {
		activities += (req -> (MutableSet(), new ListBuffer))
	}

	private def endRequest(req: String) = {
		// get and remove the activity buffer from the map
		for ((idSet, activities) <- activities.remove(req)) {
			val methods = activities.result
			writer.write(RequestActivity(req, methods))
		}
	}

	private def methodEntered(sigId: Int, threadId: Int) = {
		for {
			sig <- methodIds.get(sigId)
			(_, (idSet, activities)) <- activities
			if idSet.add(sigId)
		} activities += MethodActivity(sigId, threadId, sig)
	}

	def processDataBreak = () // don't care

	def finishProcessing() = {
		writer.finish()
	}

	def cleanup = () // noop
}