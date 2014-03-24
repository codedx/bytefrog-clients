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

package com.secdec.bytefrog.clients.common.trace

import java.io.File
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.secdec.bytefrog.clients.common.config.TraceOutputSettings
import com.secdec.bytefrog.clients.common.data.processing.processors._
import com.secdec.bytefrog.fileapi.data.TraceSegmentBuilder
import com.secdec.bytefrog.fileapi.tracefile._
import com.secdec.bytefrog.hq.data.processing.DataRouter
import com.secdec.bytefrog.hq.trace._
import com.secdec.bytefrog.hq.protocol.DefaultProtocolHelper

/** A TraceDataManager that uses a user-created "TraceOutputSettings" object
  * to configure several processors that will create entries in a trace file, and
  * optionally create a separate dump file.
  */
class FileTraceDataManager(userSettings: TraceOutputSettings) extends TraceDataManager {

	// .trace file builder that the processors will use
	private lazy val traceOutputBuilder = {
		val name = userSettings.traceName
		val date = System.currentTimeMillis
		val man = TraceManifest(name, date)
		new TraceFileBuilder(userSettings.saveFile, man)
	}

	def setupDataProcessors(router: DataRouter): Unit = {

		router ++= Seq(
			CallGraphProcessor(traceOutputBuilder),
			MethodIdProcessor(traceOutputBuilder),
			ExceptionIdProcessor(traceOutputBuilder),
			MethodCallProcessor(traceOutputBuilder, userSettings.methodCallBucketLength))

		for (dumpFile <- userSettings.dumpFile) {
			println(s"Dumping raw trace to $dumpFile")
			router += DumpFileProcessor(dumpFile, DefaultProtocolHelper.latestProtocol)
		}
	}

	def setupSegmentProcessing(startTime: Long) = {
		val segmentBuilder = new TraceSegmentBuilder
		val segmentAccess = new SegmentAccessNotifier(new DefaultSegmentAccess, { segmentBuilder += _ })
		val segmentManager = new TraceSegmentManager(segmentAccess)

		// when segmentManager.complete() is called, save the trace segments that
		// were created by the segmentBuilder, using the TraceFile api.
		val segmentsWriter = TraceFile.traceSegmentsEntry.writer(traceOutputBuilder)
		segmentManager.onComplete {
			val segments = segmentBuilder.result map { seg =>
				seg.mapStartTime(_ - startTime).mapEndTime(_ - startTime)
			}
			for (seg <- segments) segmentsWriter.write(seg)
			segmentsWriter.finish()
		}
		segmentManager
	}

	def finish(reason: TraceEndReason, traceWasStarted: Boolean): Unit = reason match {
		case TraceEndReason.Normal =>
			val finishedFile = Promise[File]
			traceOutputBuilder.getResult({ finishedFile success _ }, { finishedFile failure _ })

			// wait for the build completion callback to happen, or die with an exception
			Await.result(finishedFile.future, Duration.Inf)

		case TraceEndReason.Halted =>
			// if the trace was ended before finishing but after starting,
			// delete the dump file that may have been created
			if (traceWasStarted) {
				traceOutputBuilder.dispose()
				for (dump <- userSettings.dumpFile if dump.exists) dump.delete
			}
	}
}