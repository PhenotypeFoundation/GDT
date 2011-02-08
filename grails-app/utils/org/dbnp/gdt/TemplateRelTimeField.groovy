/**
 *  GDT, a plugin for Grails Domain Templates
 *  Copyright (C) 2011 Jeroen Wesbeek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  $Author$
 *  $Rev$
 *  $Date$
 */
package org.dbnp.gdt

class TemplateRelTimeField extends TemplateFieldTypeNew {
	static String type			= "RELTIME"
	static String casedType		= "RelTime"
	static String description	= "Relative time"
	static String category		= "Date"
	static String example		= "3w 5d 2h"

	/**
	 * Static validator closure
	 * @param fields
	 * @param obj
	 * @param errors
	 */
	static def validator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.RELTIME, { value -> (value as long) })
	}

	/**
	 * cast value to the proper type (if required and if possible)
	 * @param TemplateField field
	 * @param mixed value
	 * @return RelTime
	 * @throws IllegalArgumentException
	 */
	static RelTime castValue(org.dbnp.gdt.TemplateField field, java.lang.String value) {
		// A string was given, attempt to transform it into a timespan
		// If it cannot be parsed, set the lowest possible value of Long.
		// The validator method will raise an error
		//
		// N.B. If you try to set Long.MIN_VALUE itself, an error will occur
		// However, this will never occur: this value represents 3 bilion centuries
		try {
			return RelTime.parseRelTime(value).getValue();
		} catch (IllegalArgumentException e) {
			return Long.MIN_VALUE;
		}
	}
}
