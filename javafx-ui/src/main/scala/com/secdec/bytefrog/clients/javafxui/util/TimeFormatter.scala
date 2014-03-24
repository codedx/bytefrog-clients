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

/** A simple utility for formatting time spans in a human-readable way.
  *
  * @author robertf
  */
object TimeFormatter {
	def format(millis: Long, alwaysIncludeMinutes: Boolean = false) = {
		val minutes = (millis / 60000).toInt
		val seconds = ((millis / 1000) % 60)
		val tenths = ((millis / 100) % 10)

		if (alwaysIncludeMinutes)
			f"$minutes%02d:$seconds%02d.$tenths%01d"
		else if (minutes > 0)
			f"$minutes:$seconds%02d.$tenths%01d"
		else
			f"$seconds%2d.$tenths%01d"
	}
}