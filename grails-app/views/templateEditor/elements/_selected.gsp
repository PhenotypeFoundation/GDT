<sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
  <li class="ui-state-default <g:if test="${templateField.required}">required</g:if>" id="templateField_${templateField.id}">
    <g:render template="elements/liFieldSelected" model="['templateField': templateField, 'template': template, 'ontologies': ontologies, 'fieldTypes': fieldTypes]"/>
  </li>
</sec:ifAnyGranted>

<sec:ifNotGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
  <li class="ui-state-default" id="templateField_${templateField.id}">
    <g:render template="elements/liFieldNonEditable" model="['templateField': templateField, 'template': template, 'ontologies': ontologies, 'fieldTypes': fieldTypes]"/>
  </li>
</sec:ifNotGranted>
