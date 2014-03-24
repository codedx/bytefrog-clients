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

import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** Represents a configuration file with named sections which
  * contain lists of items or key-value pairs
  */
case class Config(sections: Map[String, ConfigSection]) {
	def section(sName: String) = sections.get(sName)

	def mapSection(sName: String) = sections.get(sName) collect { case ConfigMap(map) => map }
	def listSection(sName: String) = sections.get(sName) collect { case ConfigList(list) => list }

	def +(sectionPair: (String, ConfigSection)) = Config(sections + sectionPair)
}

/** A section inside of a Config object */
sealed trait ConfigSection

/** A list section in a Config */
case class ConfigList(items: List[String]) extends ConfigSection {
	def +(item: String) = ConfigList(items :+ item)
}
/** A map section in a Config */
case class ConfigMap(map: Map[String, String]) extends ConfigSection {
	def +(kv: (String, String)) = ConfigMap(map + kv)
}

