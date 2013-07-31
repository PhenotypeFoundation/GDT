<ul id="addOntology">
  <li onClick="$('#searchType_ncbo').attr( 'checked', true ); $( '#termID' ).val( '' );">
	<input class="check" checked type="radio" id="searchType_ncbo" name="searchType" value="ncbo">
	Search by ncboID. Fill in the ncboID of the ontology you want to add.<br />
	<input class="text" type="text" name="ncboID" id="ncboIDText" onClick="$('#searchType_ncbo').attr( 'checked', true ); $( '#termID' ).val( '' );">
	<img id="ncbo_spinner" src="${resource( dir: 'images', file: 'spinner.gif' )}" style="margin-left: 5px; display: none;">
  </li>
  <li onClick="$('#searchType_term').attr( 'checked', true ); $( '#ncboIDText' ).val( '' );">
	<input class="check" type="radio" id="searchType_term" name="searchType" value="term">
	Enter a (part of a) term. Select the desired term from the list. The ontology to which this term belongs, will 
	be added to the system.<br />
	<g:textField class="text" name="termID" rel="ontology-all" onClick="\$('#searchType_term').attr( 'checked', true ); \$( '#ncboIDText' ).val( '' );"/>
	<img id="term_spinner" src="${resource( dir: 'images', file: 'spinner.gif' )}" style="margin-left: 5px; display: none;">
  </li>
</ul>

 <script type="text/javascript">
	$(document).ready(function() {
		// initialize the ontology chooser
    	new OntologyChooser().init({
            spinner: '${resource(plugin:'gdt', dir:'images', file:'spinner.gif')}'
        });
	});
 </script>

