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

class TemplateBooleanField extends TemplateFieldTypeNew {
	static String type			= "BOOLEAN"
	static String casedType		= "Boolean"
	static String description	= "true/false"
	static String category		= "Other"
	static String example		= "A term that comes from one or more selected ontologies"

	public TemplateBooleanField() {
		println "TemplateBooleanField constructed!"
	}

	// validator
	static def validator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.BOOLEAN, { value -> (value) ? true : false })
	}
}
