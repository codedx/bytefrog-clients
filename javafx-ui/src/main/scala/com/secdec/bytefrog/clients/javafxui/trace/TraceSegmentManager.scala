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

package com.secdec.bytefrog.clients.javafxui.trace

import scala.collection.mutable.Stack
import scala.collection.JavaConversions._

import javafx.beans.property.ReadOnlyListWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections

sealed trait TraceSegmentEntryType
object TraceSegmentEntryType {
	case object SegmentBegin extends TraceSegmentEntryType
	case object SegmentEnd extends TraceSegmentEntryType
}

/** Trace segment entry
  *
  * @param entryType The `TraceSegmentEntryType` of this activity record.
  * @param depth The depth of this activity, i.e., how many levels of parents there are on the segment tree.
  * @param relativeTime The timestamp of this event, in milliseconds, relative to the start of the trace.
  * @param label The name of the trace segment.
  *
  * @author robertf
  */
case class TraceSegmentEntry(id: Int, entryType: TraceSegmentEntryType, depth: Integer, relativeTime: Long, label: String)

/** A helper to facilitate the tracking of a trace segment tree for an active trace. The trace segments are
  * stored as activities (begin and end) in a flat list for the UI component.
  *
  * @author robertf
  */
class TraceSegmentManager {
	private val entryListProperty = new ReadOnlyListWrapper(FXCollections.observableArrayList[TraceSegmentEntry])
	def entryList = entryListProperty.getReadOnlyProperty

	private val currentSegmentProperty = new ReadOnlyStringWrapper(null)
	def currentSegment = currentSegmentProperty.getReadOnlyProperty

	private val segmentStack = Stack.empty[(Int, String)]
	private var depth = 0
	private var currentSegmentID = -1

	private val segmentLabels = collection.mutable.Map[Int, String]()

	/** Report a new segment begin
	  *
	  * @param id The ID assigned to the segment
	  * @param relativeTime The timestamp of the segment start, in milliseconds, relative to the start of the trace.
	  * @param label The name of the trace segment.
	  */
	def beginSegment(id: Int, relativeTime: Long, label: String) {
		val entry = TraceSegmentEntry(id, TraceSegmentEntryType.SegmentBegin, depth, relativeTime, label)
		entryListProperty.add(entry)

		segmentLabels(id) = label

		val current = currentSegmentProperty.get
		if (current != null)
			segmentStack push currentSegmentID -> current
		currentSegmentID = id
		currentSegmentProperty set label

		depth = depth + 1
	}

	/** Report a renamed segment
	  *
	  * @param id The ID assigned to the segment
	  * @param newLabel The new label for the segment
	  */
	def renameSegment(id: Int, newLabel: String) {
		// rewrite entry list
		for {
			(segment, idx) <- entryListProperty.zipWithIndex.toList
			if segment.id == id
		} {
			entryListProperty.remove(idx)
			entryListProperty.insert(idx, segment.copy(label = newLabel))
		}

		if (segmentLabels contains id) segmentLabels(id) = newLabel

		// rewrite current segment stack
		if (currentSegmentID == id) {
			currentSegmentProperty set newLabel
		} else if (segmentStack.exists(_._1 == id)) {
			val rewritten = segmentStack.toList.map {
				case (`id`, _) => id -> newLabel
				case other => other
			}

			segmentStack.clear
			segmentStack pushAll rewritten.reverse
		}
	}

	/** Report the end of a segment
	  *
	  * @param id The ID assigned to the segment
	  * @param relativeTime The timestamp of the segment start, in milliseconds, relative to the start of the trace.
	  * @param label The name of the trace segment.
	  */
	def endSegment(id: Int, relativeTime: Long) {
		// Only act if the given id was created previously
		if (segmentLabels contains id) {
			depth = depth - 1

			val label = segmentLabels.getOrElse(id, "?")

			val entry = TraceSegmentEntry(id, TraceSegmentEntryType.SegmentEnd, depth, relativeTime, label)
			entryListProperty.add(entry)

			segmentStack.headOption match {
				case Some((id, label)) =>
					currentSegmentID = id
					currentSegmentProperty set label
					segmentStack.pop

				case None =>
					currentSegmentProperty set null
			}
		}
	}

	/** Clear the current list of segments (e.g., when starting a new trace) */
	def clear() {
		entryListProperty.clear
	}
}