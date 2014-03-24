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

package com.secdec.bytefrog.clients.javafxui.util

import java.io.File

/** Utility class for generating agent configuration arguments */
object AgentConfiguration {
	val AgentJarName = "bytefrog-tracer.jar"
	private lazy val jarPath = new File(getClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())

	def apply(hqHost: String, hqPort: Int, logFile: Option[File] = None) = {
		val prospectiveAgentLocation = new File(jarPath.getParent, AgentJarName)
		val agentJarPath = if (prospectiveAgentLocation.exists && prospectiveAgentLocation.isFile) prospectiveAgentLocation.getCanonicalPath else s"<path/to/$AgentJarName>"

		//TODO: is surrounding sub-parts of the argument valid on anything outside of Windows?
		val quotedAgentJarPath = if (agentJarPath.contains(' ')) s""""$agentJarPath"""" else agentJarPath

		val logFileParameter = logFile match {
			case Some(logFile) =>
				//TODO: is surrounding sub-parts of the argument valid on anything outside of Windows?
				val logFilePath = logFile.getCanonicalPath
				val quotedLogFilePath = if (logFilePath.contains(' ')) s""""$logFilePath"""" else logFilePath
				s";$quotedLogFilePath"

			case None =>
				""
		}

		s"-javaagent:$quotedAgentJarPath=$hqHost:$hqPort$logFileParameter"
	}
}