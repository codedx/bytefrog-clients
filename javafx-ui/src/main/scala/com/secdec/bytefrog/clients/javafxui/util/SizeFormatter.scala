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

import scala.annotation.tailrec

/** A simple utility for formatting sizes in a human-readable way.
  *
  * @author robertf
  */
object SizeFormatter {
	val SizeUnits = List("B", "KB", "MB", "GB")

	def format(bytes: Long): String = format(bytes, SizeUnits)

	@tailrec
	private def format(value: Double, units: List[String]): String = units match {
		case unit :: rest if (value > 1024 && !rest.isEmpty) => format(value / 1024, rest);
		case unit :: _ => if (unit == "GB") f"$value%.2f GB" else s"${value.toInt} $unit"
	}
}