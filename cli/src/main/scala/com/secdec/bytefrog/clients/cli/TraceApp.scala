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

import java.io.File
import java.net.InetAddress

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.secdec.bytefrog.clients.common.config._
import com.secdec.bytefrog.clients.common.trace.FileTraceDataManager
import com.secdec.bytefrog.clients.common.util.AsInt
import com.secdec.bytefrog.clients.common.util.FileHelpers.FileHelper
import com.secdec.bytefrog.hq.config._
import com.secdec.bytefrog.hq.connect.SocketServer
import com.secdec.bytefrog.hq.trace.Trace

object TraceApp {

	private val usageMessage = """Usage: TraceApp -t trace.config.file [-p server.port][ run.config.file]
	server.port        - The port number to run the HQ server on. Defaults to 8765
	trace.config.file  - The path to the trace configuration file.
	run.config.file    - The path to the run configuration file. If specified, the
	                         server will run the Java program described by the file, with
	                         the tracer agent attached. If omitted, the server will wait 
	                         for an incoming connection from a trace agent before continuing.
	"""

	/** Runs HQ for a single trace. With this script, the trace can be run in two modes:
	  * <ul>
	  * 	<li>"Remote" mode will cause HQ to wait for an incoming connection from a trace agent</li>
	  * 	<li>By specifying a "run configuration", HQ will run the described program with the
	  * 		tracer agent attached.
	  * 	</li>
	  * </ul>
	  *
	  * @param args
	  * 	-t trace.config.file - specify the trace configuration file
	  * 	-p server.port - optional: specify the port that HQ will listen on
	  * 	run.config.file - optional: if specified, HQ will run the described program
	  * 	with the tracer agent attached; otherwise it will run in "remote" mode.
	  */
	def main(args: Array[String]): Unit = {

		parseArgs(args.toList) match {
			case None =>
				println("Failed to parse args")
				println(usageMessage)
			case Some(parsedArgs) => validateArgs(parsedArgs) match {
				case Failure(e) => println(s"Argument Validation Failed: ${e.getMessage}")
				case Success(settings) =>

					println("Starting up HQ...")
					implicit val server = SocketServer.default(settings.portNum)
					server.setDaemon(true)
					server.start

					println("Starting tracing...")

					val traceFuture = Trace.getTrace(() => (settings.traceConf, settings.agentConf, settings.hqConf, settings.monitorConf))

					// start the trace immediately
					for {
						trace <- traceFuture
					} yield {
						val dataMan = new FileTraceDataManager(settings.traceOutput)
						trace.start(dataMan)
					}

					settings.runConfig match {
						case Some(cfg) => {
							val program = cfg.program
							val jarPath = cfg.agentJar.getCanonicalPath

							//figure out the parameters for the agent
							val logFile = new File(settings.traceLog).getCanonicalPath
							val thisHost = InetAddress.getLocalHost.getHostAddress

							TracedJavaProgram.run(program)(jarPath, logFile, thisHost, server.port)
						}
						case None => {
							val thisHost = InetAddress.getLocalHost.getHostAddress
							println(s"Run the application with connection settings for $thisHost:${server.port}")
						}
					}

					val traceCompletion = for {
						trace <- traceFuture
						end <- trace.completion
					} yield {
						println(s"Trace Completed! [reason: $end]")
						//println(s"\t${trace.dumpSize} bytes dumped to file")
						server.shutdown
					}

					Await.ready(traceCompletion, Duration.Inf)
					println("Goodbye!")
			}
		}

	}

	private case class CommandLineArgs(
		portNum: Int = 8765,
		traceConfigFile: Option[File] = None,
		runConfigFile: Option[File] = None)

	private case class Settings(
		portNum: Int,
		traceConf: TraceSettings,
		agentConf: AgentConfiguration,
		hqConf: HQConfiguration,
		monitorConf: MonitorConfiguration,
		traceOutput: TraceOutputSettings,
		traceLog: String,
		runConfig: Option[RunConfig])

	private def parseArgs(args: List[String]): Option[CommandLineArgs] = {
		var result: CommandLineArgs = CommandLineArgs()
		var error: Boolean = false

		//recursive processing of the args list
		def parse(list: List[String]): Unit = list match {

			case "-p" :: AsInt(p) :: tail =>
				result = result.copy(portNum = p)
				println("-p " + p)
				parse(tail)
			case "-t" :: filename :: tail =>
				val file = new File(filename)
				println("-t " + file)
				result = result.copy(traceConfigFile = Some(file))
				parse(tail)
			case runConfigFilename :: Nil =>
				val file = new File(runConfigFilename)
				println("run config = " + file)
				result = result.copy(runConfigFile = Some(file))
			case Nil => //done
			case _ =>
				println("Error: " + list)
				error = true
		}

		parse(args)

		if (error) None else Some(result)
	}

	private def validateArgs(args: CommandLineArgs): Try[Settings] = {
		val traceConfig = args.traceConfigFile
			.map(f => TraceConfigParser.parse(f.lines))
			.getOrElse(Failure(new Exception("Must specify a trace config file")))

		val runConfig = args.runConfigFile match {
			case None => Success(None)
			case Some(file) => RunConfigParser.parse(file.lines) map { Some(_) }
		}

		for {
			tcfg <- traceConfig
			cfg <- runConfig
		} yield Settings(args.portNum, tcfg.traceSettings, tcfg.agentConfig, tcfg.hqConfig, tcfg.monitorConfig, tcfg.outputSettings, tcfg.logFile getOrElse "trace.log", cfg)
	}
}