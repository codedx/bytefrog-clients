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

package com.secdec.bytefrog.clients.javafxui.views

import java.net.URL
import java.util.ResourceBundle

import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator

/** JavaFX controller class for post processing progress view
  * @author robertf
  */
class PostProcessingController extends Initializable with JavaFXUtilities {
	// these are protected because scala does weird things when they're private
	@FXML protected var progressBar: ProgressBar = _
	@FXML protected var stepProgressIndicator: ProgressIndicator = _
	@FXML protected var messageLabel: Label = _

	val model = new PostProcessingModel

	def initialize(url: URL, resources: ResourceBundle) {
		progressBar.progressProperty bind model.progress
		stepProgressIndicator.progressProperty bind model.stepProgress
		messageLabel.textProperty bind model.message
	}
}