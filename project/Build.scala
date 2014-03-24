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

import sbt._
import Keys._
import Dependencies._
import com.typesafe.sbteclipse.core.EclipsePlugin._
import sbtassembly.Plugin._
	import AssemblyKeys._
import no.vedaadata.sbtjavafx.JavaFXPlugin._

object BuildDef extends Build {

	val baseCompilerSettings = Seq(
		scalacOptions := List("-deprecation", "-unchecked", "-feature", "-target:jvm-1.6"),
		scalaVersion := "2.10.1"
	)

	val Bytefrog = file("../bytefrog")
	lazy val Agent = ProjectRef(Bytefrog, "Agent")
	lazy val Shared = ProjectRef(Bytefrog, "Common")
	lazy val HQ = ProjectRef(Bytefrog, "HQ")

	lazy val FileApi = Project("FileAPI", file("file-api"))
		.dependsOn(Shared, HQ)
		.settings(baseCompilerSettings: _*)
		.settings(
			organization := "com.avi",
			resolvers ++= dependencyResolvers,
			libraryDependencies += liftJson,
			libraryDependencies += jna,
			libraryDependencies += sbinary
		)

	lazy val Common = Project("Common", file("common"))
		.dependsOn(Shared, HQ, FileApi)
		.settings(baseCompilerSettings: _*)
		.settings(
			organization := "com.avi"
		)

	lazy val CommandLine = Project("CommandLine", file("cli"))
		.dependsOn(Shared, HQ, Common)
		.settings(baseCompilerSettings: _*)

	lazy val JavaFxUI = Project("JavaFxUI", file("javafx-ui"))
		.dependsOn(Shared, HQ, Common)
		.settings(baseCompilerSettings: _*)
		.settings(
			fork in run := true

			// uncomment for a bigger heap while tracing
			//,javaOptions in run += "-Xmx12G"

			// uncomment for trace dumps
			//,javaOptions in run += "-codepulse.enableDump=true"
		)
		.settings(jfxBaseSettings: _*)
		.settings(jfxSettings: _*)
		.settings(
			JFX.mainClass := Some("com.secdec.bytefrog.clients.javafxui.TracerApp"),
			JFX.devKit := {
				val env = System.getenv("JDK_HOME")
				if(env == null){
					println("Please set the 'JDK_HOME' environment variable to continue")
					println("  JDK_HOME should point to the directory containing the Java Development Kit.")
					println("  It will generally include directories like 'jre', 'lib', 'bin', and 'include'.")
					throw new Exception("No JDK_HOME set")
				} else {
					JFX.jdk(env)
				}
			},
			JFX.addJfxrtToClasspath := true
		)
		.settings(Distributor.distSettings: _*)
}