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

import java.io.File

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.TextInputControl

/** Some input validator wrappers for common validation types.
  * @author robertf
  */
object InputValidator {
	def numeric(control: TextInputControl): InputValidator = {
		val validator = new RegexInputValidator("""^\d+$""", control)
		control.textProperty addListener validator
		validator doValidation control.textProperty.get
		validator
	}

	def port(control: TextInputControl): InputValidator = {
		val validator = new RegexInputValidator("""^(6553[0-5]|655[0-2]\d|65[0-4]\d\d|6[0-4]\d{3}|[1-5]\d{4}|[1-9]\d{0,3}|0)$""", control)
		control.textProperty addListener validator
		validator doValidation control.textProperty.get
		validator
	}

	def nonEmpty(control: TextInputControl): InputValidator = {
		val validator = new RegexInputValidator("^.+$", control)
		control.textProperty addListener validator
		validator doValidation control.textProperty.get
		validator
	}

	def folder(control: TextInputControl): InputValidator = {
		val validator = new SavePathValidator(control)
		control.textProperty addListener validator
		validator doValidation control.textProperty.get
		validator
	}

	def validFileName(control: TextInputControl): InputValidator = {
		val validator = new RegexInputValidator("""^(?!\.)[^/\\:*?"<>|]+$""", control)
		control.textProperty addListener validator
		validator doValidation control.textProperty.get
		validator
	}
}

/** Base input validation for text controls
  * @author robertf
  */
trait InputValidator extends ChangeListener[String] {
	val valid: BooleanProperty = new SimpleBooleanProperty(true)
	val control: TextInputControl

	protected val errorStyle: String = "-fx-border-color: red"
	protected val noErrorStyle: String = "-fx-border-color: null"

	protected def doValidation(value: String) {
		val isValid = this.isValid(value)
		valid setValue isValid

		control setStyle (if (isValid) noErrorStyle else errorStyle)
	}

	def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String) {
		doValidation(newValue)
	}

	protected def isValid(value: String): Boolean
}

/** Validator that compares text to a regular expression
  * @author robertf
  */
class RegexInputValidator(inputRegex: String, val control: TextInputControl) extends InputValidator {
	def isValid(value: String) = value.matches(inputRegex)
}

/** Validator that checks if a given string is a valid save path.
  * @author robertf
  */
class SavePathValidator(val control: TextInputControl) extends InputValidator {
	def isValid(value: String) = !value.isEmpty && new File(value).getCanonicalFile.isDirectory
}