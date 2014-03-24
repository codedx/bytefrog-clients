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

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.effect.BoxBlur
import javafx.scene.effect.Effect
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window

/** Post processing progress popup
  * @author robertf
  */
class PostProcessingPopup(owner: Window) extends Stage(StageStyle.TRANSPARENT) {
	initModality(Modality.WINDOW_MODAL)
	initOwner(owner)

	private val loader = new FXMLLoader(getClass.getResource("post-processing-view.fxml"))
	val scene = new Scene(loader.load.asInstanceOf[Parent], Color.TRANSPARENT)

	setScene(scene)

	val controller = loader.getController[PostProcessingController]
	def model = controller.model

	private val ownerRoot = owner.getScene.getRoot
	private var originalEffect: Effect = _

	def showPopup() {
		originalEffect = ownerRoot.getEffect
		ownerRoot setEffect new BoxBlur(2, 2, 2)
		show

		setX(owner.getX + (owner.getWidth - getWidth) / 2)
		setY(owner.getY + (owner.getHeight - getHeight) / 2)
	}

	def closePopup() {
		ownerRoot setEffect originalEffect
		close
	}
}