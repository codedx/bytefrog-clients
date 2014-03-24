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

package com.secdec.bytefrog.fileapi.tracefile

import com.secdec.bytefrog.fileapi.data._
import com.secdec.bytefrog.fileapi.tracefile.entry._

object TraceFile {

	val methodIdEntry: TraceFileEntry[collection.Map[Int, String]] = MethodIdEntry

	val exceptionIdEntry: TraceFileEntry[collection.Map[Int, String]] = ExceptionIdEntry

	val traceSegmentsEntry: TraceFileEntry[TraceSegment] = TraceSegmentEntry

	@deprecated("Superceded by methodEncountersEntry", since = "4/29/2013")
	val encounteredMethodsEntry: TraceFileEntry[String] = EncounteredMethodsEntry

	@deprecated("Superceded by methodCallsEntry", since = "6/27/2013")
	val methodEncountersEntry: TraceFileEntry[MethodEncounters] = MethodEncountersEntry

	val methodCallsEntry: TraceFileEntry[MethodTimeBucket] = MethodCallsEntry

	val callGraphEntry: TraceFileEntry[List[CallGraphEdge]] = CallGraphEntry

	val markersEntry: TraceFileEntry[TraceMarker] = MarkersEntry

	val requestActivityEntry: TraceFileEntry[RequestActivity] = RequestActivityEntry
}