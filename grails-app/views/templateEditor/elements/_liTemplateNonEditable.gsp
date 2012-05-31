<g:set var="numUses" value="${template.numUses()}" />
<span class="listButtons">
  <img class="disabled" src="${resource( dir: 'images/icons', file: 'application_edit.png', plugin: 'famfamfam' )}" alt="Template properties are only editable for (template)Admins." title="Template properties are only editable for (template)Admins.">
  <img class="disabled" src="${resource( dir: 'images/icons', file: 'application_form.png', plugin: 'famfamfam' )}" alt="Template fields can only be removed by (template)Admins." title="Template fields can only be removed by (template)Admins.">
  <img class="disabled" src="${resource( dir: 'images/icons', file: 'page_copy.png', plugin: 'famfamfam' )}" alt="Templates can only be cloned by (template)Admins." title="Templates can only be cloned by (template)Admins.">
  <img class="disabled" src="${resource( dir: 'images/icons', file: 'delete.png', plugin: 'famfamfam' )}" alt="Templates can only be deleted by (template)Admins." title="Templates can only be deleted by (template)Admins.">
  <img onClick="requestTemplate( ${ template.id } );" src="${resource( dir: 'images/icons', file: 'email.png', plugin: 'famfamfam' )}" alt="Request a new template or template modification." title="Request a new template or template modification.">
</span>
${template.name}


<form class="templateField_form" id="template_${template.id}_form" action="updateTemplate">
	<g:hiddenField name="id" value="${template.id}" />
	<g:hiddenField name="version" value="${template.version}" />
	<g:hiddenField name="standalone" value="${standalone}" />
	<g:render template="elements/templateForm" model="['template': template]"/>
	<div class="templateFieldButtons">
		<input type="button" value="Save" onClick="updateTemplate( ${template.id} );">
		<input type="button" value="Close" onClick="hideTemplateForm( ${template.id} );">
	</div>
</form>
