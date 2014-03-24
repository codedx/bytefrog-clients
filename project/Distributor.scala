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

import sbtassembly.Plugin.AssemblyKeys._
import no.vedaadata.sbtjavafx.JavaFXPlugin.JFX._

object Distributor extends Plugin {
	val packageDistTask = TaskKey[Unit]("package-bytefrog")

	private val settingReqs = (
		name,
		crossTarget,
		packageJavaFx,
		assembly in BuildDef.Agent,
		artifactBaseNameValue
	)

	val distSettings = Seq(
		packageDistTask <<= settingReqs map { 
		(name, crossTarget, _, agentJar, artifactBaseName) =>

			val distDir = crossTarget / artifactBaseName
			val libDir = distDir / "lib"
			val jarFile = distDir / (artifactBaseName + ".jar")

			val outputFile = crossTarget / ("Bytefrog-Tracer-Files.zip")

			if(!libDir.exists || !jarFile.exists)
				sys.error("Could not find package-javafx output.")

			outputFile.delete
			if(outputFile.exists)
				sys.error("Could not delete previous distribution package. Make sure " + outputFile + " is not open.")

			println("Packaging distribution to " + outputFile + " ...")

			val libFiles = IO.listFiles(libDir) map { entry => (entry, "lib/" + entry.getName) }

			val zipMappings = 
				(jarFile, jarFile.getName) ::
				(agentJar, agentJar.getName) ::
				libFiles.toList

			IO.zip(zipMappings, outputFile)

			println("Done packaging (distribution).")
		}
	)
}
