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

package com.secdec.bytefrog.clients.cli

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

import com.secdec.bytefrog.clients.common.data.processing.processors._
import com.secdec.bytefrog.hq.data.processing.DataRouter
import com.secdec.bytefrog.fileapi.tracefile._
import com.secdec.bytefrog.hq.errors._
import com.secdec.bytefrog.hq.protocol._

import reactive.Observing

/** A simple tool to read a trace dump file and run it through data processing again. This utility
  * has minimal error checking, since it's intended as a developer tool.
  *
  * This utility will *NOT* work on old dump files; it assumes that the dump file is already sorted
  * properly. Functionality could be extended to read un-sorted dumps, inferring sequence by thread
  * ID, timestamp, and thread-local sequence ID.
  *
  * @author robertf
  */
object DumpProcessor extends Observing {
	private val usageMessage = """Usage: DumpProcessor <dump file> <output file>
	trace name       - The name of the trace (any string you want)
	dump file        - The path to the dump file to process.
	output file      - The path to save the output cptrace file to."""

	def main(args: Array[String]) {
		parseArgs(args.toList) match {
			case None =>
				println(usageMessage)

			case Some(parsedArgs) =>
				if (!parsedArgs.dumpFile.exists || !parsedArgs.dumpFile.isFile) {
					println("Dump file must point to a valid trace dump file.")
				}

				runProcessing(parsedArgs)
		}
	}

	private case class Args(traceName: String, dumpFile: File, outputFile: File)

	private def parseArgs(args: List[String]): Option[Args] = {
		args match {
			case traceName :: dumpFile :: outputFile :: Nil =>
				Some(Args(traceName, new File(dumpFile), new File(outputFile)))

			case _ => None
		}
	}

	private def runProcessing(args: Args) {
		// actually run the processing; we need to parse the dump file and run it through the data pipeline
		val dump = new DataInputStream(new BufferedInputStream(new FileInputStream(args.dumpFile)))
		val man = TraceManifest(args.traceName, System.currentTimeMillis)
		val out = new TraceFileBuilder(args.outputFile, man)

		val errorController = new TraceErrorController
		val router = new DataRouter(errorController)

		router ++= Seq(
			CallGraphProcessor(out),
			MethodIdProcessor(out),
			MethodCallProcessor(out, 1000))

		var processed = 0L
		var success = false

		for (error <- errorController.fatalErrors) {
			// everything dies on error
			println(s"[FATAL] $error")
			System exit -1
		}
		for (error <- errorController.traceWarnings) {
			println(s"[WARN] $error")
		}

		def statusUpdate {
			processed += 1

			if (processed % 1000 == 0)
				print(s"Processed $processed events\r")
		}

		val handler = new DefaultDataMessageHandler {
			import DataMessageContent._

			override def handleMapThreadName(threadName: String, threadId: Int, timestamp: Int) {
				router route MapThreadName(threadName, threadId, timestamp)
				statusUpdate
			}

			override def handleMapMethodSignature(methodSig: String, methodId: Int) {
				router route MapMethodSignature(methodSig, methodId)
				statusUpdate
			}

			override def handleMapException(exception: String, exceptionId: Int) {
				router route MapException(exception, exceptionId)
				statusUpdate
			}

			override def handleMethodEntry(methodId: Int, timestamp: Int, sequenceId: Int, threadId: Int) {
				router route MethodEntry(methodId, timestamp, threadId)
				statusUpdate
			}

			override def handleMethodExit(methodId: Int, timestamp: Int, sequenceId: Int, lineNum: Int, threadId: Int) {
				router route MethodExit(methodId, timestamp, lineNum, threadId)
				statusUpdate
			}

			override def handleExceptionMessage(exception: Int, methodId: Int, timestamp: Int, sequenceId: Int, lineNum: Int, threadId: Int) {
				router route Exception(exception, methodId, timestamp, lineNum, threadId)
				statusUpdate
			}

			override def handleExceptionBubble(exception: Int, methodId: Int, timestamp: Int, sequenceId: Int, threadId: Int) {
				router route ExceptionBubble(exception, methodId, timestamp, threadId)
				statusUpdate
			}

			override def handleMarkerMessage(timestamp: Int, sequence: Int, key: String, value: String) {
				router route Marker(key, value, timestamp)
			}

			override def handleParserError(error: Throwable) {
				errorController.reportTraceError(UnexpectedError("Parser error", error))
			}

			override def handleParserEOF {
				success = true
			}

			override def handleDataBreak {
			}
		}

		DefaultProtocolHelper.getDataMessageParser(DefaultProtocolHelper.latestProtocolVersion)
			.getOrElse(throw new NotImplementedError)
			.parse(dump, handler, parseDataBreaks = true)

		dump.close
		router.finish

		if (success) {
			val finishedResult = Promise[File]
			out.getResult({ finishedResult success _ }, { finishedResult failure _ })

			// wait for the build completion callback to happen, or die with an exception
			Await.result(finishedResult.future, Duration.Inf)

			println(s"Complete. Output saved to ${args.outputFile}")
		}
	}
}