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

object Dependencies {
	lazy val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"
	lazy val reactive= "cc.co.scala-reactive" %% "reactive-core"	% "0.3.2.1"
	lazy val jna = "net.java.dev.jna" % "jna" % "3.5.2"
	lazy val liftJson = "net.liftweb" %% "lift-json" % "2.5"
	lazy val sbinary = "org.scala-tools.sbinary" %% "sbinary" % "0.4.1"

	lazy val dependencyResolvers = Seq(
		"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
		"Typesafe IDE-2.10" at "http://repo.typesafe.com/typesafe/ide-2.10/"
	)
}