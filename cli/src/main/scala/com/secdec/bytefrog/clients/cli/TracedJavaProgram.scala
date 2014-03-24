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

import com.secdec.bytefrog.common.config.StaticAgentConfiguration

object TracedJavaProgram {

	/** Run the given `program` with the tracing agent attached.
	  * This method will also spawn threads to pipe the Standard Output
	  * and Standard Error streams from the spawned process into this process.
	  *
	  * @param program The program to be traced
	  * @param agentJarLocation The file path that describes where the tracing agent jar file exists
	  * @param logFile The file path denoting where the tracer should put its log file
	  * @param traceHost The host address of the HQ. Generally an IP address, i.e. "12.34.56.78"
	  * @param tracePort The port on HQ that expects incoming connections from the agent.
	  *
	  * @return The process that was spawned by running the program with the agent attached.
	  */
	def run(program: JavaProgram)(
		agentJarLocation: String,
		logFile: String,
		traceHost: String,
		tracePort: Int): Process = {

		//figure out the parameters for the agent
		val staticConfig = new StaticAgentConfiguration(traceHost, tracePort, logFile)
		val staticOpts = staticConfig.toOptionString
		val agentArgs = s"-javaagent:${agentJarLocation}=${staticOpts}"

		println("Agent Args: " + agentArgs)
		//run the target app, adding the agentArgs
		val agentProcess = program.copy(javaArgs = agentArgs :: program.javaArgs).exec

		new Thread(new StreamPipe(agentProcess.getInputStream, System.out)).start
		new Thread(new StreamPipe(agentProcess.getErrorStream, System.err)).start

		agentProcess
	}

}