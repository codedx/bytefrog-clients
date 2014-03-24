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

import scala.util.{ Try, Failure, Success }

/** A parser for Config objects, based on a file format like
  * {{{
  * 	[section1]
  * 	list item 1
  * 	list item 2
  * 	list item 3
  *
  * 	[section2]
  * 	key=value
  * 	key2=value2
  * 	key3=value3
  *
  * 	[section3]
  * 	another thing
  * 	and another thing
  * }}}
  */
object ConfigParser {
	class ParseException(error: String) extends Exception(error)

	/** Interprets a series of lines as a Config object, returning
	  * the result as a `scala.util.Try`. Failures will contain
	  * a `ParseException(errorMsg)`, and Successes will contain
	  * the parsed Config instance.
	  */
	def parse(lines: TraversableOnce[String]): Try[Config] = {

		val startingState = Success("" -> Config(Map()))

		val parser: ParserPF = errorParsing orElse
			blankLineParsing orElse
			headerParsing orElse
			kvLineParsing orElse
			listItemParsing orElse {
				case _ => Failure(new ParseException("Unexpected Input"))
			}

		val finalState = lines.foldLeft[ParseState](startingState) { (state, nextLine) =>
			parser(state -> nextLine)
		}

		finalState.map(_._2)
	}

	/*
	 * Implementation Note:
	 * Parsing is broken up into individual "handler" functions,
	 * which each handle a particular case. Each handler function
	 * takes a "state" and the next line to be parsed, and returns
	 * the next state. There is never any mutable state floating
	 * around somewhere.
	 */

	private val SectionHeader = raw"\[([a-zA-Z][a-zA-Z0-9 \._]*)\]".r
	private val KeyValueLine = raw"([a-zA-Z][a-zA-Z0-9]*)\s*\=\s*(.*)".r

	//ParseState = Try[currentHeader, config under construction]
	private type ParseState = Try[(String, Config)]
	private type ParserPF = PartialFunction[(ParseState, String), ParseState]

	/** If the current state is a Failure, leave it that way */
	private val errorParsing: ParserPF = {
		case (f @ Failure(_), _) => f
	}

	/** If the enxt line is blank, the state remains the same */
	private val blankLineParsing: ParserPF = {
		case (state, line) if line.trim.isEmpty => state
	}

	/** Upon encountering a section header, the state should reflect the new header */
	private val headerParsing: ParserPF = {
		case (Success((header, config)), SectionHeader(newHeader)) => Success(newHeader -> config)
	}

	/** When we encounter a key=value line, add that line to the current section, unless the current
	  * section is a "list" section, in which case that's a parse error.
	  */
	private val kvLineParsing: ParserPF = {
		case (Success((header, config)), KeyValueLine(key, value)) => config.section(header) match {
			case None =>
				val section = ConfigMap(Map(key -> value))
				val newConfig = config + (header -> section)
				Success(header -> newConfig)
			case Some(map: ConfigMap) =>
				val section = map + (key -> value)
				val newConfig = config + (header -> section)
				Success(header -> newConfig)
			case Some(list: ConfigList) =>
				Failure(new ParseException("Found a key-value pair when expecting a list item or section header"))
		}
	}

	/** When we encounter a line, treat that as a list item, and add it to the current section, unless
	  * the current section is a "map" section, in which case that's a parse error.
	  */
	private val listItemParsing: ParserPF = {
		case (Success((header, config)), line) => config.section(header) match {
			case None =>
				val section = ConfigList(List(line))
				val newConfig = config + (header -> section)
				Success(header -> newConfig)
			case Some(list: ConfigList) =>
				val section = list + line
				val newConfig = config + (header -> section)
				Success(header -> newConfig)
			case Some(map: ConfigMap) =>
				Failure(new ParseException("Found a list item when expecting a key-value pair or section header"))
		}
	}

}