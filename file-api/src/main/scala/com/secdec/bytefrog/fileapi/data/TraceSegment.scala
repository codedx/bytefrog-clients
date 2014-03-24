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

package com.secdec.bytefrog.fileapi.data

import com.secdec.bytefrog.hq.data.TraceSegmentEvent

sealed trait SegmentInfo {
	def id: Int
	def label: String
}

/** Represents a labeled segment of time, specifically used for labeling trace data.
  *
  * @param id An integer identifier for this segment. It should be unique
  * 	within the context that this segment is used.
  * @param label The name of this segment
  * @param start The start time (epoch date) of this segment
  * @param end The end time (epoch date) of this segment
  * @param children Any nested child segments whose start and end times fall within this one
  */
case class TraceSegment(id: Int, label: String, start: Long, end: Long, children: List[TraceSegment] = Nil) extends SegmentInfo {
	/** Convenience function that applies a function `f` to the `start`
	  * time of this trace and all of its children. Useful for adding
	  * or subtracting a constant value from the start times (i.e. relativizing
	  * the start time).
	  */
	def mapStartTime(f: Long => Long): TraceSegment =
		copy(start = f(start), children = children.map(_.mapStartTime(f)))

	/** Convenience function that applies a function `f` to the `end`
	  * time of this trace and all of its children. Useful for adding
	  * or subtracting a constant value from the end times (i.e. relativizing
	  * the end time).
	  */
	def mapEndTime(f: Long => Long): TraceSegment =
		copy(end = f(end), children = children.map(_.mapEndTime(f)))
}

/** Builder that takes `TraceSegmentEvent`s (e.g. OpenSegment, CloseSegment) and
  * generates a list of `TraceSegment`s.
  */
class TraceSegmentBuilder extends collection.mutable.Builder[TraceSegmentEvent, List[TraceSegment]] {
	private class SegmentPartial(val id: Int, val start: Long, var name: String) {
		var end: Option[Long] = None
		var children = List[SegmentPartial]()

		def toSegment: Option[TraceSegment] = {
			for {
				endTime <- end
				// validate that this segment is still valid in the builder
				seg <- accum.get(id) if seg eq this
			} yield {
				val fullChildren = children.flatMap { _.toSegment }
				TraceSegment(id, name, start, endTime, fullChildren)
			}
		}
	}

	private val accum = collection.mutable.Map[Int, SegmentPartial]()
	private var orphans = List.newBuilder[SegmentPartial]

	def clear() = {
		accum.clear()
	}

	def result = {
		orphans.result.flatMap { _.toSegment }
	}

	private def onSegmentOpened(id: Int, name: String, time: Long, parentId: Option[Int]) = {
		val newPartial = new SegmentPartial(id, time, name)
		accum(id) = newPartial
		val parent = for {
			id <- parentId
			seg <- accum.get(id)
		} yield seg

		parent match {
			case None => orphans += newPartial
			case Some(p) => p.children :+= newPartial
		}
	}

	def +=(event: TraceSegmentEvent) = {
		import TraceSegmentEvent._
		event match {
			case SegmentOpened(id, name, time, parentId) => onSegmentOpened(id, name, time, parentId)
			case AnonSegmentOpened(id, time, parentId) => onSegmentOpened(id, "<anon>", time, parentId)
			case SegmentClosed(id, time) => for (seg <- accum.get(id)) seg.end = Some(time)
			case SegmentRenamed(id, name) => for (seg <- accum.get(id)) seg.name = name
			case SegmentDeleted(id) => accum.remove(id)
		}
		this
	}
}