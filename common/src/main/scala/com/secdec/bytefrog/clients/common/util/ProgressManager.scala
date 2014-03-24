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

package com.secdec.bytefrog.clients.common.util

import reactive.EventStream
import reactive.EventSource

case class ProgressStatus(overallProgress: Double, message: String, stepProgress: Double)

/** Handles progress calculation for a task with sub-steps.
  *
  * @author robertf
  */
class ProgressManager(stepTimes: Double*) {
	/** An Event that gets triggered when the processor status is updated.
	  */
	def progressChange: EventStream[ProgressStatus] = progressChangeSource
	private val progressChangeSource = new EventSource[ProgressStatus]

	private var lastProgress: ProgressStatus = null
	private def fireProgress(progress: ProgressStatus) {
		// propagate if...
		if (lastProgress == null || // if this is the first progress
			(progress.overallProgress - lastProgress.overallProgress) >= 0.005 || // if overall progress has gone up by 0.5% or more
			(progress.stepProgress - lastProgress.stepProgress) >= 0.005 || // if step progress has gone up by 0.5% or more
			progress.stepProgress < 0 || // if the step progress is indeterminate
			(progress.stepProgress >= 0 && lastProgress.stepProgress < 0) // if we've changed away from being indeterminate
			) {
			progressChangeSource fire progress
			lastProgress = progress
		}
	}

	/** Fire a concrete progress within the given step.
	  *
	  * @param step The current step (starting at 1, going up to the number of stepTimes given)
	  * @param message The message to be displayed to the user describing what is being done
	  * @param stepWorkDone The amount of work done on this step
	  * @param stepWorkTotal The total amount of work on this step
	  */
	def stepProgress(step: Int, message: String, stepWorkDone: Double, stepWorkTotal: Double) {
		val doneSteps = stepTimes.take(step - 1).sum
		val partialStep = stepTimes(step - 1)

		val stepProgress = stepWorkDone / stepWorkTotal
		val overallProgress = doneSteps + partialStep * stepProgress

		fireProgress(ProgressStatus(overallProgress, message, stepProgress))
	}

	/** Fire an indeterminate progress within the given step.
	  *
	  * @param step The current step (starting at 1, going up to the number of stepTimes given)
	  * @param message The message to be displayed to the user describing what is being done
	  * @param stepProgress How far along in the current step we are, for purposes of calculating overall progress
	  */
	def stepIndeterminate(step: Int, message: String, stepProgress: Double = 0) {
		val doneSteps = stepTimes.take(step - 1).sum
		val partialStep = stepTimes(step - 1)

		val overallProgress = doneSteps + partialStep * stepProgress

		fireProgress(ProgressStatus(overallProgress, message, -1))
	}

	def endIndeterminate(message: String) {
		fireProgress(ProgressStatus(1.0, message, -1))
	}
}