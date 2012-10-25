<li id="template_${template.id}"class="ui-state-default">
  <sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
      <g:if test="${!template.inUse()}">
        <g:render template="elements/liTemplateEditable" model="['template': template, 'standalone': standalone]"/>
      </g:if>
      <g:else>
        <g:render template="elements/liTemplateNonDeletable" model="['template': template, 'standalone': standalone]"/>
      </g:else>
  </sec:ifAnyGranted>

  <sec:ifNotGranted roles="ROLE_ADMIN, ROLE_TEMPLATEADMIN">
      <g:render template="elements/liTemplateNonEditable" model="['template': template, 'standalone': standalone]"/>
  </sec:ifNotGranted>
</li>