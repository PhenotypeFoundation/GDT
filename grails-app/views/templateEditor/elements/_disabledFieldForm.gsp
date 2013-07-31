	<g:hiddenField name="id" value="${templateField?.id}" />
	<g:hiddenField name="version" value="${templateField?.version}" />
	<g:hiddenField name="is_disabled" value="1" />
	<g:if test="${is_selected}"><g:hiddenField name="renderTemplate" value="selected" /></g:if>
	<g:if test="${template}"><g:hiddenField name="templateId" value="${template.id}" /></g:if>

	<label for="name">Name:</label> <g:textField disabled="disabled" name="name" value="${templateField?.name}" /><br />
	<label for="type">Type:</label> <g:textField disabled="disabled" name="type" value="${templateField?.type}" /><br />

	<div class="extra extendablestringlist_options stringlist_options" <g:if test="${templateField?.type.toString() == 'STRINGLIST' || templateField?.type.toString() == 'EXTENDABLESTRINGLIST'}">style='display: block;'</g:if>>
	  <label for="type">Used items:</label>
		<g:textArea name="usedListEntries" disabled="disabled" value="${templateField?.getUsedListEntries().name?.join( '\n' )}" />
	  <label for="type">Extra Items (every item on a new line):</label>
		<g:textArea name="listEntries" value="${templateField?.getNonUsedListEntries().name?.join( '\n' )}" />
	</div>
	<div class="extra ontologyterm_options" <g:if test="${templateField?.type.toString() == 'ONTOLOGYTERM'}">style='display: block;'</g:if>>
	  <label for="type">Used ontologies:</label>
        <g:select size="5" from="${templateField?.getUsedOntologies()}" disabled="true" class="ontologySelect" optionValue="name" optionKey="id" name="ontologies" id="used_ontologies_${templateField?.id}" /> <br />

	  <label for="type">Extra ontologies:<br /><a href="#" style="text-decoration: underline;" onClick="openOntologyDialog();">Add new</a><br /><br /> <a href="#" style="text-decoration: underline;" onClick="deleteOntology(${templateField?.id});">Remove</a></label>
		<g:select multiple="yes" size="5" from="${ (templateField?.ontologies ?: [])- templateField?.getUsedOntologies()}" class="ontologySelect" optionValue="name" optionKey="id" name="ontologies" id="ontologies_${templateField?.id}" /><br />
	</div>
	
	<label for="unit">Unit:</label> <g:textField disabled="disabled" name="unit" value="${templateField?.unit}" /><br />
	<label for="comment">Comment:</label> <g:textArea name="comment" value="${templateField?.comment}" /><br />
	<label for="required">Required:</label> <input type="checkbox" disabled <g:if test="${templateField?.required}">checked</g:if><br />

	<div class="templateFieldButtons">
	  <g:if test="${ templateField?.type.toString() == 'STRINGLIST' || templateField?.type.toString() == 'EXTENDABLESTRINGLIST' || templateField?.type.toString() == 'ONTOLOGYTERM' }">
		<input type="button" value="Save" onClick="updateTemplateField( ${templateField?.id} );">
		<input type="button" value="Close" onClick="hideTemplateFieldForm( ${templateField?.id} );">
	  </g:if>
	  <g:else>
		<input type="button" value="Close" onClick="hideTemplateFieldForm( ${templateField?.id} );">
	  </g:else>
	</div>
