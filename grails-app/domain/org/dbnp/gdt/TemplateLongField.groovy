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

class TemplateLongField extends TemplateFieldTypeNew {
	static String type			= "LONG"
	static String casedType		= "Long"
	static String description	= "Natural number"
	static String category		= "Numerical"
	static String example		= "100"

	/**
	 * Static validator closure
	 * @param fields
	 * @param obj
	 * @param errors
	 */
	static def validator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.LONG, { value -> value.toLong() }, { value -> Long.parseLong(value.trim()) })
	}

	/**
	 * cast value to the proper type (if required and if possible)
	 * @param TemplateField field
	 * @param mixed value
	 * @return Long
	 * @throws IllegalArgumentException
	 */
	public castValue(TemplateField field,value) {
		if (value && value.class == String) {
			// A check for invalidity is done in the validator of these fields. For that
			// reason, we just try to parse it here. If it fails, the validator will also
			// fail.
			try {
				value = Long.parseLong(value.trim())
			} catch( Exception e ) {
				throw new IllegalArgumentException("Argument could not be cast to Long: ${value}")
			}
		}

		return value
	}
}
