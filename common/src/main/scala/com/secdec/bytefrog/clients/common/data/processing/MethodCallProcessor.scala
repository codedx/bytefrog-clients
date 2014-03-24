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

import com.secdec.bytefrog.fileapi.data.MethodBucketRecord
import com.secdec.bytefrog.fileapi.data.MethodTimeBucket
import com.secdec.bytefrog.fileapi.tracefile.TraceFile
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder
import com.secdec.bytefrog.hq.data.processing.DataProcessor
import com.secdec.bytefrog.hq.protocol.DataMessageContent

object MethodCallProcessor {
	def apply(output: TraceFileBuilder, bucketLength: Int) = new MethodCallProcessor(output, bucketLength)
}

/** The MethodCallProcessor counts how many times each method was called during each
  *
  * @author robertf
  */
class MethodCallProcessor(output: TraceFileBuilder, bucketLength: Int) extends DataProcessor {
	private var currentBucket = 0
	private val callCounts = mutable.Map[Int, Long]() withDefaultValue 0

	private val writer = TraceFile.methodCallsEntry.writer(output)

	def processMessage(message: DataMessageContent) = message match {
		case DataMessageContent.MethodEntry(method, ts, _) =>
			val bucket = ts / bucketLength

			if (bucket != currentBucket) {
				saveCurrentBucket
				currentBucket = bucket
			}

			callCounts(method) += 1

		case _ =>
	}

	def processDataBreak {
		// we don't care :)
	}

	def finishProcessing() {
		saveCurrentBucket
		writer.finish
	}

	def cleanup() {
		writer.finish
	}

	private def saveCurrentBucket() {
		if (!callCounts.isEmpty)
			saveBucket(currentBucket, callCounts)

		callCounts.clear
	}

	private def saveBucket(bucket: Int, callCounts: collection.Map[Int, Long]) {
		val bucketRecord = MethodTimeBucket(bucket * bucketLength, (bucket + 1) * bucketLength,
			callCounts.map(pair => MethodBucketRecord(pair._1, pair._2)).toList)

		writer write bucketRecord
	}
}