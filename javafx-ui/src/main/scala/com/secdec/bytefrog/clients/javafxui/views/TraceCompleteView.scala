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

/** Trace complete view
  * @author robertf
  */
class TraceCompleteView {
	private val loader = new FXMLLoader(getClass.getResource("trace-complete-view.fxml"))
	val scene = new Scene(loader.load.asInstanceOf[Parent])
	val controller = loader.getController[TraceCompleteController]
	def model = controller.model
}