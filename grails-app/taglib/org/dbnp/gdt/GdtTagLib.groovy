/**
 *  GDT, a plugin for Grails Domain Templates
 *  Copyright (C) 2011 Jeroen Wesbeek, Kees van Bochove
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

import nl.grails.plugins.ajaxflow.AjaxflowTagLib

class GdtTagLib extends AjaxflowTagLib {
	def gdtService

	// define default text field width
	static defaultTextFieldSize = 25;

	/**
	 * create a dynamic menu to edit Templates
	 * @param attrs
	 */
	def templateEditorMenu = { attrs ->
		// find all domain entities that use the Grails Domain Templates
		gdtService.getTemplateEntities().each {
			//out << "${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}<a href=\"${resource()}/templateEditor?entity=${encryptedEntity}&standalone=true\">${readableClassName} templates</a>${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}"
			out << "${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}<a href=\"${resource()}/templateEditor?entity=${it.encoded}&standalone=true\">${it.description} templates</a>${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}"
		}

		// export and import links
		out << "${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}<a href=\"${resource()}/template/export\">Export</a>${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}\n"
		out << "${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}<a href=\"${resource()}/template/importTemplate\">Import</a>${((attrs.get('wrap')) ? '<' + attrs.get('wrap') + '>' : '')}\n"
	}

	/**
	 * generate a base form element
	 * @param String inputElement name
	 * @param Map attributes
	 * @param Closure help content
	 */
	def baseElement = { inputElement, attrs, help ->
		log.info ".rendering [" + inputElement + "] with name [" + attrs.get('name') + "] and value [" + ((attrs.value) ? attrs.get('value').toString() : "-") + "]"

		// work variables
		def description = attrs.remove('description')
		def addExampleElement = attrs.remove('addExampleElement')
		def addExample2Element = attrs.remove('addExample2Element')
		def helpText = help().trim()

		// execute inputElement call
		def renderedElement = "$inputElement"(attrs)

		// if false, then we skip this element
		if (!renderedElement) return false

		// render a form element
		out << '<div class="element' + ((attrs.get('required')) ? ' required' : '') + '"' + ((attrs.get('elementId')) ? 'id="' + attrs.remove('elementId') + '"' : '') + '>'
		out << ' <div class="description">'
		out << ((description) ? description.replaceAll(/[a-z][A-Z][a-z]/) { it[0] + ' ' + it[1..2] }.replaceAll(/\w+/) { it[0].toUpperCase() + ((it.size() > 1) ? it[1..-1] : '') } : '')
		out << ' </div>'
		out << ' <div class="input">'
		out << renderedElement
		out << ((helpText.size() > 0) ? '	<div class="helpIcon"></div>' : '')

		// add an disabled input box for feedback purposes
		// @see dateElement(...)
		if (addExampleElement) {
			def exampleAttrs = new LinkedHashMap()
			exampleAttrs.name = attrs.get('name') + 'Example'
			exampleAttrs.class = 'isExample'
			exampleAttrs.disabled = 'disabled'
			exampleAttrs.size = 30
			out << textField(exampleAttrs)
		}

		// add an disabled input box for feedback purposes
		// @see dateElement(...)
		if (addExample2Element) {
			def exampleAttrs = new LinkedHashMap()
			exampleAttrs.name = attrs.get('name') + 'Example2'
			exampleAttrs.class = 'isExample'
			exampleAttrs.disabled = 'disabled'
			exampleAttrs.size = 30
			out << textField(exampleAttrs)
		}

		out << ' </div>'

		// add help content if it is available
		if (helpText.size() > 0) {
			out << '  <div class="helpContent">'
			out << '    ' + helpText
			out << '  </div>'
		}

		out << '</div>'
	}

	/**
	 * bind an ajax submit to an onChange event
	 * @param attrs
	 * @return attrs
	 */
	private getAjaxOnChange = { attrs ->
		// work variables
		def internetExplorer = (request.getHeader("User-Agent") =~ /MSIE/)
		def ajaxOnChange = attrs.remove('ajaxOnChange')

		// is ajaxOnChange defined
		if (ajaxOnChange) {
			if (!attrs.onChange) attrs.onChange = ''

			// add onChange AjaxSubmit javascript
			if (internetExplorer) {
				// 		- somehow IE submits these onchanges twice which messes up some parts of the wizard
				//		  (especially the events page). In order to bypass this issue I have introduced an
				//		  if statement utilizing the 'before' and 'after' functionality of the submitToRemote
				//		  function. This check expects lastRequestTime to be in the global Javascript scope,
				//		  (@see pageContent) and calculates the time difference in miliseconds between two
				//		  onChange executions. If this is more than 100 miliseconds the request is executed,
				//		  otherwise it will be ignored... --> 20100527 - Jeroen Wesbeek
				attrs.onChange += ajaxSubmitJs(
					[
						before: "var execute=true;try { var currentTime=new Date().getTime();execute = ((currentTime-lastRequestTime) > 100);lastRequestTime=currentTime;  } catch (e) {};if (execute) { 1",
						after: "}",
						functionName: ajaxOnChange,
						url: attrs.get('url'),
						update: attrs.get('update'),
						afterSuccess: attrs.get('afterSuccess')
					],
					''
				)
			} else {
				// this another W3C browser that actually behaves as expected... damn you IE, DAMN YOU!
				attrs.onChange += ajaxSubmitJs(
					[
						functionName: ajaxOnChange,
						url: attrs.get('url'),
						update: attrs.get('update'),
						afterSuccess: attrs.get('afterSuccess')
					],
					''
				)
			}
		}

		return attrs
	}

	/**
	 * render an ajaxButtonElement
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def ajaxButtonElement = { attrs, body ->
		baseElement.call(
			'ajaxButton',
			attrs,
			body
		)
	}

	/**
	 * render a textFieldElement
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def textFieldElement = { attrs, body ->
		// set default size, or scale to max length if it is less than the default size
		if (!attrs.get("size")) {
			if (attrs.get("maxlength")) {
				attrs.size = ((attrs.get("maxlength") as int) > defaultTextFieldSize) ? defaultTextFieldSize : attrs.get("maxlength")
			} else {
				attrs.size = defaultTextFieldSize
			}
		}

		// render template element
		baseElement.call(
			'textField',
			attrs,
			body
		)
	}

	/**
	 * render a textAreaElement
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def textAreaElement = { attrs, body ->
		// set default size, or scale to max length if it is less than the default size

		// render template element
		baseElement.call(
			'textArea',
			attrs,
			body
		)
	}

	/**
	 * render a select form element
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def selectElement = { attrs, body ->
		baseElement.call(
			'select',
			attrs,
			body
		)
	}

	/**
	 * render a checkBox form element
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def checkBoxElement = { attrs, body ->
		baseElement.call(
			'checkBox',
			attrs,
			body
		)
	}

	/**
	 * render a set of radio form elements
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def radioElement = { attrs, body ->
		baseElement.call(
			'radioList',
			attrs,
			body
		)
	}

	/**
	 * render a set of radio elements
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def radioList = { attrs ->
		def checked = true

		attrs.elements.each {
			out << radio(
				name: attrs.name,
				value: it,
				checked: (attrs.value == it || (!attrs.value && checked))
			)
			out << it
			checked = false
		}
	}

	/**
	 * render a dateElement
	 * NOTE: datepicker is attached through wizard.js!
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def dateElement = { attrs, body ->
		// transform value?
		if (attrs.value instanceof Date) {
			// transform date instance to formatted string (dd/mm/yyyy)
			attrs.value = String.format('%td/%<tm/%<tY', attrs.value)
		}

		// add 'rel' field to identity the datefield using javascript
		attrs.rel = 'date'

		// set some textfield values
		attrs.maxlength = (attrs.maxlength) ? attrs.maxlength : 10
		attrs.addExampleElement = true

		// render a normal text field
		//out << textFieldElement(attrs,body)
		textFieldElement.call(
			attrs,
			body
		)
	}

	/**
	 * render a dateElement
	 * NOTE: datepicker is attached through wizard.js!
	 * @param Map attrs
	 * @param Closure body  (help text)
	 */
	def timeElement = { attrs, body ->
		// transform value?
		if (attrs.value instanceof Date) {
			// transform date instance to formatted string (dd/mm/yyyy)
			attrs.value = String.format('%td/%<tm/%<tY %<tH:%<tM', attrs.value)
		}

		// add 'rel' field to identity the field using javascript
		attrs.rel = 'datetime'

		attrs.addExampleElement = true
		attrs.addExample2Element = true
		attrs.maxlength = 16

		// render a normal text field
		//out << textFieldElement(attrs,body)
		textFieldElement.call(
			attrs,
			body
		)
	}

	/**
	 * Button form element
	 * @param Map attributes
	 * @param Closure help content
	 */
	def buttonElement = { attrs, body ->
		// render template element
		baseElement.call(
			'ajaxButton',
			attrs,
			body
		)
	}

	/**
	 * Template form element
	 * @param Map attributes
	 * @param Closure help content
	 */
	def templateElement = { attrs, body ->
		// render template element
		baseElement.call(
			'templateSelect',
			attrs,
			body
		)
	}

	/**
	 * render a template select element
	 * @param Map attrs
	 */
	def templateSelect = { attrs ->
		def entity = attrs.remove('entity')

		// enctrypt entity
		attrs['entity'] = gdtService.encryptEntity(entity.toString())

		// fetch templates
		attrs.from = (entity) ? Template.findAllByEntity(entity) : Template.findAll()

		// got a name?
		if (!attrs.name) {
			attrs.name = 'template'
		}

		// add a rel element if it does not exist
		if (!attrs.rel) {
			attrs.rel = 'template'
		}

		// got an ajaxOnChange defined?
		attrs = getAjaxOnChange.call(
			attrs
		)

		// got result?
		if (attrs.from.size() > 0 || attrs.get('addDummy')) {
			// transform all values into strings
			def from = []
			attrs.from.each { from[from.size()] = it.toString() }

			// sort alphabetically
			from.sort()

			// add a dummy field?
			if (attrs.remove('addDummy')) {
				from.add(0, '')
			}

			// set attributes
			attrs.from = from
			attrs.value = (attrs.value) ? attrs.value.toString() : ''

			// output select element
			out << select(attrs)
		} else {
			// no, return false to make sure this element
			// is not rendered in the template
			return false
		}
	}

	/**
	 * File form element
	 * @param Map attributes
	 * @param Closure help content
	 */
	def fileFieldElement = { attrs, body ->
		// render term element
		baseElement.call(
			'fileField',
			attrs,
			body
		)
	}

	/**
	 * file field.
	 * @param attributes
	 */
	def fileField = { attrs ->
		/*
		out << '<input type="file" name="' + attrs.name + '"/>'
		if( attrs.value ) {
			out << '<a href="' + resource(dir: '') + '/file/get/' + attrs.value + '" class="isExample">Now contains: ' + attrs.value + '</a>'
		}
		*/

		out << '<div id="upload_button_' + attrs.name + '" class="upload_button">Upload</div>';
		out << '<input type="hidden" name="' + attrs.name + '" id="' + attrs.name + '" value="existing*' + attrs.value + '">';
		out << '<div id="' + attrs.name + 'Example" class="upload_info"></div>';
		out << '<a id="' + attrs.name + 'Delete" class="upload_del" href="#" onClick="if( confirm( \'Are you sure to delete this file?\' ) ) { deleteFile( \'' + attrs.name + '\' ); } return false;"><img src="' + resource(dir: 'images/icons', file: 'delete.png', plugin: 'famfamfam') + '"></a>';
		out << '<script type="text/javascript">';
		out << '  $(document).ready( function() { ';
		out << '    var filename = "' + attrs.value + '";';
		out << '    fileUploadField( "' + attrs.name + '" );';
		out << '    if( filename != "" ) {';
		out << '      $("#' + attrs.name + 'Delete").show();';
		out << '      $("#' + attrs.name + 'Example").html("Current file: " + createFileHTML( filename ) )';
		out << '    }';
		out << '  } );';
		out << "</script>\n";
	}

	/**
	 * show a formatted value of a certain template field
	 * @param Map attributes
	 */
	def showTemplateField = { attrs ->
		def field = attrs.get('field');
		def entity = attrs.get('entity');
		def fieldName = '';
		def fieldType = '';
		def fieldUnit = '';

		if (entity) {
			if (field instanceof String) {
				fieldName = field;
				fieldType = '';
				fieldUnit = '';
			} else if (field instanceof TemplateField) {
				fieldName = field.name
				fieldType = field.type.toString();
				fieldUnit = field.unit
			} else {
				return;
			}

			def value = entity.getFieldValue(fieldName);

			// Show a link if the field is a FILE field
			if (fieldType == 'FILE' && value != "") {
				out << '<a href="' + g.createLink(controller: "file", action: "get", id: value) + '">' + value + '</a>';
			} else if (fieldType == 'RELTIME') {
				out << new RelTime(value).toString()
			} else {
				out << value;
			}

			// Show the unit (if a unit is present and a value was shown)
			if (fieldUnit && value != null && value != "")
			out << " " + fieldUnit

		}
	}

	/**
	 * render table headers for all subjectFields in a template
	 * @param Map attributes
	 */
	def templateColumnHeaders = { attrs ->
		def entity = (attrs.get('entity'))
		def template = (entity && entity instanceof TemplateEntity) ? entity.template : null
		def columnWidths = (attrs.get('columnWidths')) ? attrs.remove('columnWidths') : []

		// got a template?
		if (template) {
			// render template fields
			entity.giveFields().each() {
				// Format the column name by:
				// - separating combined names (SampleName --> Sample Name)
				// - capitalizing every seperate word
				def ucName = it.name.replaceAll(/[a-z][A-Z][a-z]/) {
					it[0] + ' ' + it[1..2]
				}.replaceAll(/\w+/) {
					it[0].toUpperCase() + ((it.size() > 1) ? it[1..-1] : '')
				}

				// strip spaces
				def ucNameSpaceless = ucName.replaceAll(/ /) { '' }

				// do we have to use a specific width for this column?
				if (columnWidths[ucName]) {
					out << '<div class="' + attrs.get('class') + '" style="width:' + columnWidths[ucNameSpaceless] + 'px;" rel="resized">' + ucName + (it.unit ? " (${it.unit})" : '')
				} else {
					out << '<div class="' + attrs.get('class') + '">' + ucName + (it.unit ? " (${it.unit})" : '')
				}
				if (it.comment) {
					out << '<div class="helpIcon"></div>'
					out << '<div class="helpContent">' + it.comment + '</div>'
				}
				out << '</div>'
			}
		}
	}

	/**
	 * render all template fields of an entity as table columns
	 * @param Map attributes
	 */
	def templateColumns = { attrs ->
		// render template fields as columns
		attrs.renderType = 'column'
		out << renderTemplateFields(attrs)
	}

	/**
	 * render all template fields of an entity as a web form
	 * @param Map attributes
	 */
	def templateElements = { attrs ->
		// render template fields as form elements
		attrs.renderType = 'element'
		out << renderTemplateFields(attrs)
	}

	/**
	 * render form elements based on an entity's template
	 * @param Map attributes
	 * @param String body
	 */
	def renderTemplateFields = { attrs ->
		def renderType = attrs.remove('renderType')
		def entity = (attrs.get('entity'))
		def prependName = (attrs.get('name')) ? attrs.remove('name') + '_' : ''
		def template = (entity && entity instanceof TemplateEntity) ? entity.template : null
		def inputElement = null
		def addDummy = (attrs.get('addDummy')) ? true : false
		def ignore = attrs.get( 'ignore' );
		
		if( ignore instanceof String )
			ignore = [ignore]
		
		// Put all ignored fields into lower case, in order to perform a case insensitive search
		ignore = ignore.collect { it.toLowerCase() }

		// got a template?
		if (template) {
			// render template fields
			entity.giveFields().each() {
				// Check whether this field should be ignored. This is true if its name occurs in the ignore parameter
				// and the field is not required
				if( it.isRequired() || !ignore || !ignore.contains( it.name?.toLowerCase() ) ) {
					
					def fieldValue = entity.getFieldValue(it.name)
					def helpText = (it.comment && renderType == 'element') ? it.comment : ''
					def ucName = it.name[0].toUpperCase() + it.name.substring(1)
	
					// output column opening element?
					if (renderType == 'column') {
						out << '<div class="' + attrs.get('class') + '">'
					}
	
					switch (it.type.toString()) {
						case ['STRING', 'DOUBLE', 'LONG']:
							inputElement = (renderType == 'element') ? 'textFieldElement' : 'textField'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: fieldValue,
								required: it.isRequired()
							) {helpText}
							break
						case 'TEXT':
							inputElement = (renderType == 'element') ? 'textAreaElement' : 'textField'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: fieldValue,
								required: it.isRequired()
							) {helpText}
							break
						case 'STRINGLIST':
							inputElement = (renderType == 'element') ? 'selectElement' : 'select'
							if (!it.listEntries.isEmpty()) {
								out << "$inputElement"(
									description: ucName,
									name: prependName + it.escapedName(),
									from: it.listEntries,
									value: fieldValue,
									required: it.isRequired()
								) {helpText}
							} else {
								out << '<span class="warning">no values!!</span>'
							}
							break
						case 'ONTOLOGYTERM':
							// @see http://www.bioontology.org/wiki/index.php/NCBO_Widgets#Term-selection_field_on_a_form
							// @see ontology-chooser.js
							inputElement = (renderType == 'element') ? 'termElement' : 'termSelect'
	
							// override addDummy to always add the dummy...
							addDummy = true
	
							if (it.ontologies) {
								out << "$inputElement"(
									description: ucName,
									name: prependName + it.escapedName(),
									value: fieldValue.toString(),
									ontologies: it.ontologies,
									addDummy: addDummy,
									required: it.isRequired()
								) {helpText}
							} else {
								out << "$inputElement"(
									description: ucName,
									name: prependName + it.escapedName(),
									value: fieldValue.toString(),
									addDummy: addDummy,
									required: it.isRequired()
								) {helpText}
							}
							break
						case 'DATE':
							inputElement = (renderType == 'element') ? 'dateElement' : 'textField'
	
							// transform value?
							if (fieldValue instanceof Date) {
								if (fieldValue.getHours() == 0 && fieldValue.getMinutes() == 0) {
									// transform date instance to formatted string (dd/mm/yyyy)
									fieldValue = String.format('%td/%<tm/%<tY', fieldValue)
								} else {
									// transform to date + time
									fieldValue = String.format('%td/%<tm/%<tY %<tH:%<tM', fieldValue)
								}
							}
	
							// render element
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: fieldValue,
								rel: 'date',
								required: it.isRequired()
							) {helpText}
							break
						case ['RELTIME']:
							inputElement = (renderType == 'element') ? 'textFieldElement' : 'textField'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: new RelTime(fieldValue).toString(),
								addExampleElement: true,
								onBlur: 'showExampleReltime(this)',
								required: it.isRequired()
							) {helpText}
							break
						case ['FILE']:
							inputElement = (renderType == 'element') ? 'fileFieldElement' : 'fileField'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: fieldValue ? fieldValue : "",
								addExampleElement: true,
								required: it.isRequired()
							) {helpText}
							break
						case ['BOOLEAN']:
							inputElement = (renderType == 'element') ? 'checkBoxElement' : 'checkBox'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								value: fieldValue,
								required: it.isRequired()
							) {helpText}
							break
						case ['TEMPLATE']:
							inputElement = (renderType == 'element') ? 'templateElement' : 'templateSelect'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								addDummy: true,
								entity: it.entity,
								value: fieldValue,
								required: it.isRequired()
							) {helpText}
							break
						case ['MODULE']:
							def from = []
							AssayModule.findAll().each { from[from.size()] = it.toString() }
	
							inputElement = (renderType == 'element') ? 'selectElement' : 'select'
							out << "$inputElement"(
								description: ucName,
								name: prependName + it.escapedName(),
								from: from,
								value: fieldValue.toString(),
								required: it.isRequired()
							) {helpText}
							break
							break
						default:
							// unsupported field type
							out << '<span class="warning">!' + it.type + '</span>'
							break
					}
	
					// output column closing element?
					if (renderType == 'column') {
						out << '</div>'
					}
				} // end if ignored
			}
		}
	}
}