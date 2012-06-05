<li id="template_${template.id}"class="ui-state-default">
  <g:set var="templateAdmin" value="${false}"/>
  <sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
      <g:if test="${!template.inUse()}">
        <g:render template="elements/liTemplateEditable" model="['template': template, 'standalone': standalone]"/>
      </g:if>
      <g:else>
        <g:render template="elements/liTemplateNonDeletable" model="['template': template, 'standalone': standalone]"/>
      </g:else>
      <g:set var="templateAdmin" value="${true}"/>
  </sec:ifAnyGranted>

  <g:if test="${!templateAdmin}">
      <g:render template="elements/liTemplateNonEditable" model="['template': template, 'standalone': standalone]"/>
  </g:if>
</li>