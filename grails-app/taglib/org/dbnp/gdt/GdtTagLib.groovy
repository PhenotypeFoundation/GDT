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

	/**
	 * create a dynamic menu to edit Templates
	 * @param attrs
	 */
	def templateEditorMenu = { attrs ->
		// find all domain entities that use the Grails Domain Templates
		gdtService.getTemplateEntities().each {
			//out << "${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}<a href=\"${resource()}/templateEditor?entity=${encryptedEntity}&standalone=true\">${readableClassName} templates</a>${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}"
			out << "${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}<a href=\"${resource()}/templateEditor?entity=${it.encoded}&standalone=true\">${it.description} templates</a>${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}"
		}

		// export and import links
		out << "${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}<a href=\"${resource()}/template/export\">Export</a>${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}\n"
		out << "${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}<a href=\"${resource()}/template/importTemplate\">Import</a>${((attrs.get('wrap')) ? '<'+attrs.get('wrap')+'>' : '')}\n"
	}
}