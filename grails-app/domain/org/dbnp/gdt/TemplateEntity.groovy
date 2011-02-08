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

// temporary import until bgdt refactoring is done
import org.dbnp.bgdt.*
import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * The TemplateEntity domain Class is a superclass for all template-enabled study capture entities, including
 * Study, Subject, Sample and Event. This class provides functionality for storing the different TemplateField
 * values and returning the combined list of 'domain fields' and 'template fields' of a TemplateEntity.
 * For an explanation of those terms, see the Template class.
 *
 * @see package org.dbnp.gdt.Template
 *
 * Revision information:
 * $Rev: 1284 $
 * $Author: work@osx.eu $
 * $Date: 2010-12-20 15:48:26 +0100 (Mon, 20 Dec 2010) $
 */
abstract class TemplateEntity extends Identity {
	def gdtService

	// allow the usage of searchable, set to
	// false by default
	static searchable = false

	// The actual template of this TemplateEntity instance
	Template template

	// Maps for storing the different template field values
	Map templateStringFields		= [:]
	Map templateTextFields			= [:]
	Map templateStringListFields	= [:]
	Map templateDoubleFields		= [:]
	Map templateDateFields			= [:]
	Map templateBooleanFields		= [:]
	Map templateTemplateFields		= [:]
	Map templateModuleFields		= [:]
	Map templateLongFields			= [:]

	// N.B. If you try to set Long.MIN_VALUE for a reltime field, an error will occur
	// However, this will never occur in practice: this value represents 3 bilion centuries
	Map templateRelTimeFields		= [:] // Contains relative times in seconds
	Map templateFileFields			= [:] // Contains filenames
	Map templateTermFields			= [:]

	// define relationships
	static hasMany = [
		templateStringFields		: String,
		templateTextFields			: String,
		templateStringListFields	: TemplateFieldListItem,
		templateDoubleFields		: double,
		templateDateFields			: Date,
		templateTermFields			: Term,
		templateRelTimeFields		: long,
		templateFileFields			: String,
		templateBooleanFields		: boolean,
		templateTemplateFields		: Template,
		templateModuleFields		: AssayModule,
		templateLongFields			: long,
		systemFields				: TemplateField
	]

	// remember required fields when
	// so we can validate is the required
	// template fields are set
	Template requiredFieldsTemplate	= null
	Set requiredFields				= []

	/**
	 * Get the required fields for the defined template, currently
	 * this method is called in custom validators below but it's
	 * best to call it in a template setter method. But as that
	 * involves a lot of refactoring this implementation will do
	 * fine for now.
	 *
	 * Another possible issue might be that if the template is
	 * updated after the required fields are cached in the object.
	 *
	 * @return Set 	requiredFields
	 */
	final Set getRequiredFields() {
		// check if template is set
		if (template && !template.equals(requiredFieldsTemplate)) {
			// template has been set or was changed, fetch
			// required fields for this template
			requiredFields			= template.getRequiredFields()
			requiredFieldsTemplate	= template
		} else if (!template) {
			// template is not yet set, or was reset
			requiredFieldsTemplate	= null
			requiredFields			= []
		}


		// return the required fields
		return requiredFields
	}

	// overload transients from Identity and append requiredFields vars
	static transients	= [ "identifier", "iterator", "maximumIdentity", "requiredFields", "requiredFieldsTemplate", "searchable" ]

	// define the mapping
	static mapping = {
		// Specify that each TemplateEntity-subclassing entity should have its own tables to store TemplateField values.
		// This results in a lot of tables, but performance is presumably better because in most queries, only values of
		// one specific entity will be retrieved. Also, because of the generic nature of these tables, they can end up
		// containing a lot of records (there is a record for each entity instance for each property, instead of a record
		// for each instance as is the case with 'normal' straightforward database tables. Therefore, it's better to split
		// out the data to many tables.
		tablePerHierarchy false

		// Make sure that the text fields are really stored as TEXT, so that those Strings can have an arbitrary length.
		templateTextFields type: 'text'
	}

	// Inject the service for storing files (for TemplateFields of TemplateFieldType FILE).
	//def fileService

	/**
	 * Constraints
	 *
	 * All template fields have their own custom validator. Note that there
	 * currently is a lot of code repetition. Ideally we don't want this, but
	 * unfortunately due to scope issues we cannot re-use the code. So make
	 * sure to replicate any changes to all pieces of logic! Only commented
	 * the first occurrence of the logic, please refer to the templateStringFields
	 * validator if you require information about the validation logic...
	 */
	static constraints = {
		template(nullable: true, blank: true)
		templateStringFields(validator		: TemplateStringField.validator)
		templateTextFields(validator		: TemplateTextField.validator)
		templateStringListFields(validator	: TemplateStringListField.validator)
		templateDoubleFields(validator		: TemplateDoubleField.validator)
		templateDateFields(validator		: TemplateDateField.validator)
		templateRelTimeFields(validator		: TemplateRelTimeField.validator)
		templateTermFields(validator		: TemplateOntologyTermField.validator)
		templateFileFields(validator		: TemplateFileField.validator)
		templateBooleanFields(validator		: TemplateBooleanField.validator)
		templateTemplateFields(validator	: TemplateTemplateField.validator)
		templateModuleFields(validator		: TemplateModuleField.validator)
		templateLongFields(validator		: TemplateLongField.validator)
	}

	/**
	 * Get the proper templateFields Map for a specific field type
	 * @param TemplateFieldType
	 * @return pointer
	 * @visibility private
	 * @throws NoSuchFieldException
	 */
	public Map getStore(TemplateFieldType fieldType) {
		try {
			return this."template${fieldType.casedName}Fields"
		} catch (Exception e) {
			throw new NoSuchFieldException("Field type ${fieldType} not recognized")
		}
		/*
		if (this.metaClass.hasMetaProperty("template${fieldType.casedName}Fields")) {
			return this."template${fieldType.casedName}Fields"
		} else {
			throw new NoSuchFieldException("Field type ${fieldType} not recognized")
		}
		*/
	}

	/**
	 * Find a field domain or template field by its name and return its description
	 * @param fieldsCollection the set of fields to search in, usually something like this.giveFields()
	 * @param fieldName The name of the domain or template field
	 * @return the TemplateField description of the field
	 * @throws NoSuchFieldException If the field is not found or the field type is not supported
	 */
	private static TemplateField getField(List<TemplateField> fieldsCollection, String fieldName) {
		// escape the fieldName for easy matching
		// (such escaped names are commonly used
		// in the HTTP forms of this application)
		String escapedLowerCaseFieldName = fieldName.toLowerCase().replaceAll("([^a-z0-9])", "_")

		// Find the target template field, if not found, throw an error
		TemplateField field = fieldsCollection.find { it.name.toLowerCase().replaceAll("([^a-z0-9])", "_") == escapedLowerCaseFieldName }

		if (field) {
			return field
		}
		else {
			throw new NoSuchFieldException("Field ${fieldName} not recognized")
		}
	}

	/**
	 * Find a domain or template field by its name and return its value for this entity
	 * @param fieldName The name of the domain or template field
	 * @return the value of the field (class depends on the field type)
	 * @throws NoSuchFieldException If the field is not found or the field type is not supported
	 */
	def getFieldValue(String fieldName) {

		if (isDomainField(fieldName)) {
			return this[fieldName]
		}
		else {
			TemplateField field = getField(this.giveTemplateFields(), fieldName)
			return getStore(field.type)[fieldName]
		}
	}

	/**
	 * Check whether a given template field exists or not
	 * @param fieldName The name of the template field
	 * @return true if the given field exists and false otherwise
	 */
	boolean fieldExists(String fieldName) {
		// getField should throw a NoSuchFieldException if the field does not exist
		try {
			TemplateField field = getField(this.giveFields(), fieldName)
			// return true if exception is not thrown (but double check if field really is not null)
			if (field) {
				return true
			}
			else {
				return false
			}
		}
		// if exception is thrown, return false
		catch (NoSuchFieldException e) {
			return false
		}
	}

	/**
	 * Set a template/entity field value
	 * @param fieldName The name of the template or entity field
	 * @param value The value to be set, this should align with the (template) field type, but there are some convenience setters
	 */
	def setFieldValue(String fieldName, value) {
		def grailsApplication = ApplicationHolder.application

		// get the template field
		def TemplateField field = getField(this.giveFields(), fieldName)

		// try to cast the field to the proper type
		if (value.class == String) {
			try {
				//def instance = grailsApplication.getAllClasses().find{it.name =~ "Template${field.type.casedName}Field"}
				def templateFieldClass = gdtService.getTemplateFieldTypeByCasedName(field.type.casedName)

				// and cast the value to the proper type
				value = templateFieldClass.castValue(field,value)

			} catch (Exception e) {
				throw new IllegalArgumentException("invalid argument '${value}' of type ${value.class} when setting ${field.type.casedName} field ${fieldName}")
			}
		}
println ".setting ${field.type.casedName}: ${fieldName}='${value}' :: ${value.class}"

		// Set the field value
		if (isDomainField(field)) {
			// got a value?
			if (value) {
				this[field.name] = value
			} else {
				// remove value. For numbers, this is done by setting
				// the value to 0, otherwise, setting it to NULL
				switch (field.type.toString()) {
					case [ 'DOUBLE', 'RELTIME', 'LONG']:
						this[field.name] = 0;
						break;
					case [ 'BOOLEAN' ]:
						this[field.name] = false;
						break;
					default:
						this[field.name] = null
				}
			}
		} else {
			// Caution: this assumes that all template...Field Maps are already initialized (as is done now above as [:])
			// If that is ever changed, the results are pretty much unpredictable (random Java object pointers?)!
			def store = getStore(field.type)

			// If some value is entered (or 0 or BOOLEAN false), then save the value
			// otherwise, it should not be present in the store, so
			// it is unset if it is.
			if (value || value == 0 || ( field.type == TemplateFieldType.BOOLEAN && value == false)) {
				// set value
				store[fieldName] = value
			} else if (store[fieldName]) {
				// remove the item from the Map (if present)
				store.remove(fieldName)
			}
		}

		return this
	}

	/**
	 * Check if a given field is a domain field
	 * @param TemplateField field instance
	 * @return boolean
	 */
	boolean isDomainField(TemplateField field) {
		return isDomainField(field.name)
	}

	/**
	 * Check if a given field is a domain field
	 * @param String field name
	 * @return boolean
	 */
	boolean isDomainField(String fieldName) {
		return this.giveDomainFields()*.name.contains(fieldName)
	}

	/**
	 * Return all fields defined in the underlying template and the built-in
	 * domain fields of this entity
	 */
	def List<TemplateField> giveFields() {
		return this.giveDomainFields() + this.giveTemplateFields();
	}

	/**
	 * Return all templated fields defined in the underlying template of this entity
	 */
	def List<TemplateField> giveTemplateFields() {
		return (this.template) ? this.template.fields : []
	}

	def TemplateField getField( fieldName ) {
		return getField(this.giveFields(), fieldName);
	}

	/**
	 * Look up the type of a certain template field
	 * @param String fieldName The name of the template field
	 * @return String The type (static member of TemplateFieldType) of the field, or null of the field does not exist
	 */
	TemplateFieldType giveFieldType(String fieldName) {
		def field = giveFields().find {
			it.name == fieldName
		}
		field?.type
	}

	/**
	 * Return all relevant 'built-in' domain fields of the super class. Should be implemented by a static method
	 * @return List with DomainTemplateFields
	 * @see TemplateField
	 */
	abstract List<TemplateField> giveDomainFields()

	/**
	 * Convenience method. Returns all unique templates used within a collection of TemplateEntities.
	 *
	 * If the collection is empty, an empty set is returned. If none of the entities contains
	 * a template, also an empty set is returned.
	 */
	static Collection<Template> giveTemplates(Collection<TemplateEntity> entityCollection) {
		def set = entityCollection*.template?.unique();

		// If one or more entities does not have a template, the resulting
		// set contains null. That is not what is meant.
		set = set.findAll { it != null };

		// Sort the list so we always have the same order
		set = set.sort{ a, b ->
			a == null || b == null || a.equals(b) ? 0 :
			a.name < b.name ? -1 :
			a.name > b.name ?  1 :
			a.id < b.id ? -1 : 1
		}

		return set
	}

	/**
	 * Convenience method. Returns the template used within a collection of TemplateEntities.
	 * @throws NoSuchFieldException when 0 or multiple templates are used in the collection
	 * @return The template used by all members of a collection
	 */
	static Template giveSingleTemplate(Collection<TemplateEntity> entityCollection) {
		def templates = giveTemplates(entityCollection);
		if (templates.size() == 0) {
			throw new NoSuchFieldException("No templates found in collection!")
		} else if (templates.size() == 1) {
			return templates[0];
		} else {
			throw new NoSuchFieldException("Multiple templates found in collection!")
		}
	}

    /**
     * Returns a Class object given by the entityname, but only if it is a subclass of TemplateEntity
	 *
	 * @return A class object of the given entity, null if the entity is not a subclass of TemplateEntity
	 * @throws ClassNotFoundException
     */
    static Class parseEntity( String entityName ) {
		if( entityName == null )
			return null

        // Find the templates
        def entity = Class.forName(entityName, true, Thread.currentThread().getContextClassLoader())

        // succes, is entity an instance of TemplateEntity?
        if (entity?.superclass =~ /TemplateEntity$/ || entity?.superclass?.superclass =~ /TemplateEntity$/) {
            return entity;
        } else {
            return null;
        }

    }
}
