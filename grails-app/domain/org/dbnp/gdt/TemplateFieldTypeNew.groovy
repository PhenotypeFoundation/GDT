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

abstract class TemplateFieldTypeNew implements Serializable {
	// inject the GdtService
	def gdtService

	/**
	 * class constructor
	 */
	def public TemplateFieldTypeNew() {
		// make sure the GdtService is available
		if (!gdtService) {
			gdtService = new GdtService()
		}

		// register this templateFieldType with TemplateEntity
		gdtService.registerTemplateFieldType(this)
	}

	/**
	 * transients
	 */
	static transients = ["type", "casedType", "description", "category", "example"]

	/**
	 * Magic setter
	 * @param mixed value
	 */
	//abstract public castValue(value)

	/**
	 * Static validator closure
	 * @param fields
	 * @param obj
	 * @param errors
	 */
	//abstract validator(fields, obj, errors)

	/**
	 * Generic Validator
	 * @param fields
	 * @param obj
	 * @param errors
	 * @param templateFieldType
	 * @param castClosure
	 * @param parseClosure
	 * @param extraValidationClosure
	 * @return
	 */
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure) {
		genericValidator(fields, obj, errors, templateFieldType, castClosure, { value -> throw new Exception('dummy')}, { value -> return true })
	}
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure) {
		genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure, { value -> return true })
	}
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure, extraValidationClosure) {
		def error = false
		def fieldTypeName = templateFieldType.toString()
		def lowerFieldTypeName = fieldTypeName.toLowerCase()
		def capitalizedFieldTypeName = lowerFieldTypeName[0].toUpperCase() + lowerFieldTypeName.substring(1)

		// catch exceptions
		try {
			// iterate through values
			fields.each { key, value ->
				// check if the value exists and is of the proper type
				if (value) {
					// check if it is of the proper type
					if (value.class.toString().toLowerCase() != lowerFieldTypeName) {
						// no, try to cast value
						try {
							fields[key] = castClosure(value)
						} catch (Exception castException) {
							// could not cast value to the proper type, try to parse value
							try {
								fields[key] = parseClosure(value)
							} catch (Exception parseException) {
								// cannot cast nor parse value, invalid value
								error = true
								errors.rejectValue(
									"template${capitalizedFieldTypeName}Fields",
									"templateEntity.typeMismatch.${lowerFieldTypeName}",
									[key, value.class] as Object[],
									"Property {0} must be of type ${fieldTypeName} and is currently of type {1}"
								)
							}
						}
					} else {
						// yes, try extra validation
						// 	- return boolean: validation success
						//	- return string: validation failed (contains i18n translation
						//    location, e.g. templateEntity.tooLong.string)
						def extraValidation = extraValidationClosure(value)
						if (extraValidation.class == String) {
							error = true
							errors.rejectValue(
								"template${capitalizedFieldTypeName}Fields",
								extraValidation,
								[key] as Object[],
								"Property {0} does not pass extra validation (${extraValidation})"
							)
						}
					}
				}
			}

			// validating required fields
			obj.getRequiredFields().findAll { it.type == templateFieldType }.each { field ->
				if (!fields.find { key, value -> key == field.name }) {
					// required field is missing
					error = true
					errors.rejectValue(
						"template${capitalizedFieldTypeName}Fields",
						'templateEntity.required',
						[field.name] as Object[],
						'Property {0} is required but it missing'
					)
				}
			}
		} catch (Exception e) {
			println "Exception in the genericValidators: ${e.getMessage()}"
			println e.stackTrace
		}

		return (!error)
	}
}