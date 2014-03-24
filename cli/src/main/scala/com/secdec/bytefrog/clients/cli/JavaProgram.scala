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

case class JavaProgram(
	mainClass: String,
	javaArgs: List[String] = Nil,
	classPath: List[String] = Nil,
	programArgs: List[String] = Nil) {

	def exec = {
		val cmd = "java"

		val classpathArg = classPath match {
			case Nil => Nil
			case cp => List("-cp", cp mkString System.getProperty("path.separator"))
		}

		val args = List(cmd) ++ javaArgs ++ classpathArg ++ List(mainClass) ++ programArgs
		val pb = new ProcessBuilder(args: _*)

		pb.start
	}

}