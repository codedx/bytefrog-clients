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
import scala.util.{ Try, Success, Failure }

/** Encapsulates the information required to run a java program
  * with a trace agent attached.
  *
  * @param program The unmodified program to run
  * @param agentJar The trace agent jar file
  */
case class RunConfig(program: JavaProgram, agentJar: File)

/** An object that knows how to parse a [[RunConfig]] object from a run configuration file. */
object RunConfigParser {

	/** Parses a "run configuration" file, which uses the [[Config]] format to specify
	  * information about the trace's settings.
	  *
	  * There are two required sections in the file:
	  * <ul>
	  * 	<li>
	  * 		"target" - this section must specify the "mainClass" property, which names
	  * 		the main class of the target java program.
	  * 	</li>
	  * 	<li>
	  * 		"agent" - this section must specify the "jar" property, which is the
	  * 		file path to the tracer agent jar.
	  * 	</li>
	  * </ul>
	  *
	  * There are three optional sections:
	  * <ul>
	  * 	<li>
	  * 		"target.javaArgs" - a list of arguments to supply as arguments to "java"
	  * 		when running the target java program
	  * 	</li>
	  * 	<li>
	  * 		"target.classpath" - a list of files/jars/directories to supply as the
	  * 		classpath to the target java program
	  * 	</li>
	  * 	<li>
	  * 		"target.args" - a list of arguments to supply to the target java program
	  * 	</li>
	  * </ul>
	  *
	  * The optional sections help construct the command line to run the java program, along the lines of
	  * `java {target.javaArgs} -cp {target.classpath} {mainClass} {target.args}`
	  *
	  * @param lines A series of text lines representing the contents of the config file.
	  */
	def parse(lines: TraversableOnce[String]): Try[RunConfig] = {
		for {
			config <- ConfigParser parse lines
			result <- parse(config)
		} yield result
	}

	/** Helper for the public `parse` method
	  */
	private def parse(config: Config): Try[RunConfig] = {

		val spec = for {
			//get the target section's mainClass attribute
			targetMap <- config mapSection "target"
			mainClass <- targetMap get "mainClass"

			//get the argent section's jar attribute
			agentMap <- config mapSection "agent"
			agentJar <- agentMap get "jar"
		} yield {

			val javaArgs = config listSection "target.javaArgs" getOrElse Nil
			val classpath = config listSection "target.classpath" getOrElse Nil
			val appArgs = config listSection "target.args" getOrElse Nil

			val javaProgram = JavaProgram(mainClass, javaArgs, classpath, appArgs)
			val agentJarFile = new File(agentJar)

			RunConfig(javaProgram, agentJarFile)
		}

		spec match {
			case Some(s) => Success(s)
			case None => Failure(new IllegalArgumentException("incorrect program spec"))
		}
	}

}