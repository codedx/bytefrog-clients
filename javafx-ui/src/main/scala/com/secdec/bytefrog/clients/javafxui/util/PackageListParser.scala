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

import java.util.regex.Pattern

import scala.util.parsing.combinator.RegexParsers

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.control.TextInputControl

/** A parser for user entered package filters that generates regular expressions for
  * the agent. Input format is one filter per line. Typing just a (fully qualified)
  * package name includes only classes that are direct children of the package, and
  * putting a wildcard will include any children packages as well.
  *
  * E.g., 'com.secdec' will not include com.secdec.bytefrog.foo, whereas 'com.secdec.*' would.
  *
  * @author robertf
  */
object PackageListParser extends RegexParsers {
	private trait PackageToken
	private case class Identifier(name: String) extends PackageToken
	private case object PackageOnlyWildcard extends PackageToken
	private case object Wildcard extends PackageToken

	private def renderToken(token: PackageToken) = token match {
		case Identifier(name) => Pattern.quote(name)
		case PackageOnlyWildcard => "[^/]+"
		case Wildcard => ".+"
	}

	/** override white space, so it doesn't eat new lines */
	override val whiteSpace = """[ \t]+""".r

	/** Parser that accepts java identifier strings */
	private val identifier = rep1 {
		acceptIf(Character.isJavaIdentifierPart _)(c => s"Unexpected: ${c}")
	} ^^ (id => Identifier(id.mkString))

	/** Parser that accepts wildcards */
	private val wildcard = "*" ^^^ Wildcard

	/** Either a java identifier or a wildcard */
	private val packagePart: Parser[PackageToken] = identifier | wildcard

	/** A series of package parts, separated by a ., representing a package filter */
	private val packageFilter = rep1sep(packagePart, ".") ^^ { filters =>
		val renderedTokens = filters.map(renderToken)

		// we only add the package-only wildcard if there is no trailing wildcard
		if (filters.last == Wildcard)
			renderedTokens.mkString("^", "/", "$")
		else
			renderedTokens.mkString("^", "/", s"/${renderToken(PackageOnlyWildcard)}$$")
	}

	/** New lines separate package filters */
	private val newLine = "\r" | "\n"

	/** A list of package filters, separated by new lines */
	private val packageList = repsep(packageFilter, rep1(newLine)) <~ rep(newLine)

	def apply(input: String) = parseAll(packageList, input) match {
		case Success(result, _) => Some(result)
		case _ => None
	}
}

/** Helper class that parses a package filter control's contents as well as validating them.
  * @robertf
  */
class PackageListParser(val control: TextInputControl) extends InputValidator {
	private val packageListProperty = new ReadOnlyObjectWrapper[List[String]]
	def packageList = packageListProperty.getReadOnlyProperty

	control.textProperty addListener this
	doValidation(control.textProperty.get)

	def isValid(value: String) = {
		val parsed = PackageListParser(value)

		parsed match {
			case Some(parsed) if (!parsed.isEmpty) =>
				packageListProperty set parsed
				true

			case _ =>
				packageListProperty set List()
				false
		}
	}
}