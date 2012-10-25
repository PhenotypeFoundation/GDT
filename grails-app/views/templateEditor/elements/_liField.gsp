<sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
    <g:if test="${templateField.inUse()}">
    	<g:render template="elements/liFieldInUse" model="['templateField': templateField, 'ontologies': ontologies, 'fieldTypes': fieldTypes]"/>
    </g:if>
    <g:else>
    	<g:render template="elements/liFieldNotInUse" model="['templateField': templateField, 'ontologies': ontologies, 'fieldTypes': fieldTypes]"/>
    </g:else>
</sec:ifAnyGranted>

<sec:ifNotGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
    <g:render template="elements/liFieldNonEditable" model="['templateField': templateField, 'ontologies': ontologies, 'fieldTypes': fieldTypes]"/>
</sec:ifNotGranted>

