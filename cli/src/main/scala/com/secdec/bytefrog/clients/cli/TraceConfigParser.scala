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
import scala.util.Try

import com.secdec.bytefrog.clients.common.config._
import com.secdec.bytefrog.clients.common.util.AsInt
import com.secdec.bytefrog.clients.common.util.AsLong
import com.secdec.bytefrog.hq.config._

/** An object that knows how to parse a trace configuration file. */
object TraceConfigParser {

	case class ParsedConfig(
		traceSettings: TraceSettings,
		agentConfig: AgentConfiguration,
		hqConfig: HQConfiguration,
		monitorConfig: MonitorConfiguration,
		outputSettings: TraceOutputSettings,
		logFile: Option[String])

	/** Parses a "trace configuration" file, which uses the [[Config]] format to specify
	  * information about the trace's settings.
	  *
	  * The configuration uses four different headers:
	  * <ul>
	  * 	<li>"exclusions" - each line in this section describes the classes to be excluded from the trace</li>
	  * 	<li>"inclusions" - each line in this section describes the classes to be included in the trace</li>
	  * 	<li>
	  * 		"agent properties" - each line in this section is a "key=value" pair, specifying a
	  * 		setting that pertains specifically to the tracing agent. See [[AgentConfiguration]]
	  * 	</li>
	  * 	<li>
	  * 		"trace properties" - each line in this section is a "key=value" pair, specifying a
	  * 		setting that pertains specifically to the HQ side of the trace. See [[TraceSettings]]
	  * 	</li>
	  * </ul>
	  *
	  * @lines A series of text lines representing the contents of the config file.
	  * @return A `Success` containing the parsed [[TraceSettings]] and output settings
	  * as a tuple, or a `Failure` if the parsing encountered an error.
	  */
	def parse(lines: TraversableOnce[String]): Try[ParsedConfig] = {
		for {
			config <- ConfigParser.parse(lines)
		} yield {

			// collect the lines from the "exclusions" section as a List, defaulting to Nil
			val exclusions = config.section("exclusions") collect {
				case ConfigList(list) => list
			} getOrElse Nil

			// collect the lines from the "inclusions" section as a List, defaulting to Nil
			val inclusions = config.section("inclusions") collect {
				case ConfigList(list) => list
			} getOrElse Nil

			// collect the lines from the "agent properties" section as a Map, defaulting to an empty Map
			val agentProps = config.section("agent properties") collect {
				case ConfigMap(map) => map
			} getOrElse Map()

			// collect the lines from the "trace properties" section as a Map, defaulting to an empty Map
			val traceProps = config.section("trace properties") collect {
				case ConfigMap(map) => map
			} getOrElse Map()

			// create a default AgentConfiguration, then modify it according to each 
			// agent configuration setting found in the "agentProps" map
			val agentConfig = agentProps.foldLeft(AgentConfiguration()) {
				case (config, kv) => kv match {
					case ("heartbeatInterval", AsInt(i)) => config.copy(heartbeatInterval = i)
					case ("bufferMemoryBudget", AsInt(i)) => config.copy(bufferMemoryBudget = i)
					case ("poolRetryCount", AsInt(i)) => config.copy(poolRetryCount = i)
					case ("numDataSenders", AsInt(i)) => config.copy(numDataSenders = i)
					case _ => config
				}
			}

			// create default TraceSettings components, initialized with the previously-discovered
			// inclusions and exclusions, then modify it according to each trace configuration
			// setting found in the "traceProps" map
			val hqConfig = traceProps.foldLeft(HQConfiguration()) {
				case (config, kv) => kv match {
					case ("dataQueueMaximumSize", AsInt(i)) => config.copy(dataQueueMaximumSize = i)
					case ("sortQueueInitialSize", AsInt(i)) => config.copy(sortQueueInitialSize = i)
					case _ => config
				}
			}

			val monitorConfig = traceProps.foldLeft(MonitorConfiguration(heartbeatInterval = agentConfig.heartbeatInterval)) {
				case (config, kv) => kv match {
					case ("dumpSizeWarning", AsLong(i)) => config.copy(dumpSizeWarning = i)
					case ("minRemainingSpace", AsLong(i)) => config.copy(minRemainingSpace = i)
					case ("heartbeatInterval", AsInt(i)) => config.copy(heartbeatInterval = i)
					case ("maxMissedHeartbeats", AsInt(i)) => config.copy(maxMissedHeartbeats = i)
					case ("modeChangeDelay", AsInt(i)) => config.copy(modeChangeDelay = i)
					case _ => config
				}
			}

			val traceOutputConfiguration = traceProps.foldLeft(TraceOutputSettings("Trace")) {
				case (config, kv) => kv match {
					case ("traceName", name) => config.copy(traceName = name)
					case ("dumpFile", filename) => config.copy(dumpFile = Some(new File(filename).getCanonicalFile))
					case ("saveFile", filename) => config.copy(saveFile = new File(filename).getCanonicalFile)
					case ("methodCallBucketLength", AsInt(len)) => config.copy(methodCallBucketLength = len)
					case _ => config
				}
			}

			val logFile = traceProps.get("logFile")

			val traceSettings = TraceSettings(exclusions, inclusions)

			// return the trace settings and output settings as a Pair
			ParsedConfig(traceSettings, agentConfig, hqConfig, monitorConfig, traceOutputConfiguration, logFile)
		}
	}
}