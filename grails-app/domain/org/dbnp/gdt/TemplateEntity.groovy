package org.dbnp.gdt

// temporary import until bgdt refactoring is done
import org.dbnp.bgdt.*

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
	def fileService

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
		templateStringFields(validator		: templateStringFieldsValidator)
		templateTextFields(validator		: templateTextFieldsValidator)
		templateStringListFields(validator	: templateStringListFieldsValidator)
		templateDoubleFields(validator		: templateDoubleFieldsValidator)
		templateDateFields(validator		: templateDateFieldsValidator)
		templateRelTimeFields(validator		: templateRelTimeFieldsValidator)
		templateTermFields(validator		: templateTermFieldsValidator)
		templateFileFields(validator		: templateFileFieldsValidator)
		templateBooleanFields(validator		: templateBooleanFieldsValidator)
		templateTemplateFields(validator	: templateTemplateFieldsValidator)
		templateModuleFields(validator		: templateModuleFieldsValidator)
		templateLongFields(validator		: templateLongFieldsValidator)
	}

	// text fields validator
	static def templateTextFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.STRING, { value -> (value as String) })
	}

	// long fields validator
	static def templateLongFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.LONG, { value -> value.toLong() }, { value -> Long.parseLong(value.trim()) })
	}

	// string fields validator
	static def templateStringFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.STRING, { value -> (value as String) }, { value -> throw new Exception('dummy') }, { value -> println "aapjes"; return (value.class == String && value.size() > 255) ? 'templateEntity.tooLong.string' : true })
	}

	// stringlist fields validator
	static def templateStringListFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldListItem, { value -> (value as TemplateFieldListItem) })
	}

	// double fields validator
	static def templateDoubleFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.DOUBLE, { value -> (value.toDouble()) }, { value -> println "obsolete?"; return (value as Double) })
	}

	// date fields validator
	static def templateDateFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.DATE, { value -> (value as Date) })
	}

	// reltime fields validator
	static def templateRelTimeFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.RELTIME, { value -> (value as long) })
	}

	// term fields validator
	static def templateTermFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.ONTOLOGYTERM, { value -> (value as Term) })
	}

	// file fields validator
	static def templateFileFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.FILE, { value -> (value as String) })
		// currently the validator only casts to String, perhaps we also
		// need to look on the filesystem if the file actually exists using
		// the 'extraValidationClosure' ?
	}

	// boolean fields validator
	static def templateBooleanFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.BOOLEAN, { value -> (value) ? true : false })
	}

	// template fields validator
	static def templateTemplateFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.TEMPLATE, { value -> (value as Template) })
	}

	// module fields validator
	static def templateModuleFieldsValidator = { fields, obj, errors ->
		genericValidator(fields, obj, errors, TemplateFieldType.MODULE, { value -> (value as AssayModule) })
	}

	/**
	 * Generic Validator
	 * @param fields
	 * @param obj
	 * @param errors
	 * @param templateFieldType
	 * @param castClosure
	 * @param parseClosure
	 * @param extraValidationClosure
	 * @return
	 */
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure) {
		genericValidator(fields, obj, errors, templateFieldType, castClosure, { value -> throw new Exception('dummy')}, { value -> return true })
	}
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure) {
		genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure, { value -> return true })
	}
	static def genericValidator(fields, obj, errors, templateFieldType, castClosure, parseClosure, extraValidationClosure) {
		def error = false
		def fieldTypeName = templateFieldType.toString()
		def lowerFieldTypeName = fieldTypeName.toLowerCase()
		def capitalizedFieldTypeName = lowerFieldTypeName[0].toUpperCase() + lowerFieldTypeName.substring(1)

		// catch exceptions
		try {
			// iterate through values
			fields.each { key, value ->
				// check if the value exists and is of the proper type
				if (value) {
					// check if it is of the proper type
					if (value.class.toString().toLowerCase() != lowerFieldTypeName) {
						// no, try to cast value
						try {
							fields[key] = castClosure(value)
						} catch (Exception castException) {
							// could not cast value to the proper type, try to parse value
							try {
								fields[key] = parseClosure(value)
							} catch (Exception parseException) {
								// cannot cast nor parse value, invalid value
								error = true
								errors.rejectValue(
									"template${capitalizedFieldTypeName}Fields",
									"templateEntity.typeMismatch.${lowerFieldTypeName}",
									[key, value.class] as Object[],
									"Property {0} must be of type ${fieldTypeName} and is currently of type {1}"
								)
							}
						}
					} else {
						// yes, try extra validation
						// 	- return boolean: validation success
						//	- return string: validation failed (contains i18n translation
						//    location, e.g. templateEntity.tooLong.string)
						def extraValidation = extraValidationClosure(value)
						if (extraValidation.class == String) {
							error = true
							errors.rejectValue(
								"template${capitalizedFieldTypeName}Fields",
								extraValidation,
								[key] as Object[],
								"Property {0} does not pass extra validation (${extraValidation})"
							)
						}
					}
				}
			}

			// validating required fields
			obj.getRequiredFields().findAll { it.type == templateFieldType }.each { field ->
				if (!fields.find { key, value -> key == field.name }) {
					// required field is missing
					error = true
					errors.rejectValue(
						"template${capitalizedFieldTypeName}Fields",
						'templateEntity.required',
						[field.name] as Object[],
						'Property {0} is required but it missing'
					)
				}
			}
		} catch (Exception e) {
			println "Exception in the genericValidators: ${e.getMessage()}"
			println e.stackTrace
		}

		return (!error)
	}

	/**
	 * Get the proper templateFields Map for a specific field type
	 * @param TemplateFieldType
	 * @return pointer
	 * @visibility private
	 * @throws NoSuchFieldException
	 */
	public Map getStore(TemplateFieldType fieldType) {
		switch (fieldType) {
			case TemplateFieldType.STRING:
				return templateStringFields
			case TemplateFieldType.TEXT:
				return templateTextFields
			case TemplateFieldType.STRINGLIST:
				return templateStringListFields
			case TemplateFieldType.DATE:
				return templateDateFields
			case TemplateFieldType.RELTIME:
				return templateRelTimeFields
			case TemplateFieldType.FILE:
				return templateFileFields
			case TemplateFieldType.DOUBLE:
				return templateDoubleFields
			case TemplateFieldType.ONTOLOGYTERM:
				return templateTermFields
			case TemplateFieldType.BOOLEAN:
				return templateBooleanFields
			case TemplateFieldType.TEMPLATE:
				return templateTemplateFields
			case TemplateFieldType.MODULE:
				return templateModuleFields
			case TemplateFieldType.LONG:
				return templateLongFields
			default:
				throw new NoSuchFieldException("Field type ${fieldType} not recognized")
		}
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
		// get the template field
		TemplateField field = getField(this.giveFields(), fieldName)

		// Convenience setter for boolean fields
		if( field.type == TemplateFieldType.BOOLEAN && value && value.class == String ) {
			def lower = value.toLowerCase()
			if (lower.equals("true") || lower.equals("on") || lower.equals("x")) {
				value = true
			}
			else if (lower.equals("false") || lower.equals("off") || lower.equals("")) {
				value = false
			}
			else {
				throw new IllegalArgumentException("Boolean string not recognized: ${value} when setting field ${fieldName}")
			}
		}

		// Convenience setter for template string list fields: find TemplateFieldListItem by name
		if (field.type == TemplateFieldType.STRINGLIST && value && value.class == String) {
			def escapedLowerCaseValue = value.toLowerCase().replaceAll("([^a-z0-9])", "_")
			value = field.listEntries.find {
				it.name.toLowerCase().replaceAll("([^a-z0-9])", "_") == escapedLowerCaseValue
			}

			if (!value) {
				throw new IllegalArgumentException("Stringlist item not recognized: ${escapedLowerCaseValue} when setting field ${fieldName}")
			}
		}

		// Magic setter for dates: handle string values for date fields
		if (field.type == TemplateFieldType.DATE && value && value.class == String) {
			// a string was given, attempt to transform it into a date instance
			// and -for now- assume the dd/mm/yyyy format
			def dateMatch = value =~ /^([0-9]{1,})([^0-9]{1,})([0-9]{1,})([^0-9]{1,})([0-9]{1,})((([^0-9]{1,})([0-9]{1,2}):([0-9]{1,2})){0,})/
			if (dateMatch.matches()) {
				// create limited 'autosensing' datetime parser
				// assume dd mm yyyy  or dd mm yy
				def parser = 'd' + dateMatch[0][2] + 'M' + dateMatch[0][4] + (((dateMatch[0][5] as int) > 999) ? 'yyyy' : 'yy')

				// add time as well?
				if (dateMatch[0][7] != null) {
					parser += dateMatch[0][8] + 'HH:mm'
				}

				value = new Date().parse(parser, value)
			}
		}

		// Magic setter for relative times: handle string values for relTime fields
		if (field.type == TemplateFieldType.RELTIME && value != null && value.class == String) {
			// A string was given, attempt to transform it into a timespan
			// If it cannot be parsed, set the lowest possible value of Long.
			// The validator method will raise an error
			//
			// N.B. If you try to set Long.MIN_VALUE itself, an error will occur
			// However, this will never occur: this value represents 3 bilion centuries
			try {
				value = RelTime.parseRelTime(value).getValue();
			} catch (IllegalArgumentException e) {
				value = Long.MIN_VALUE;
			}
		}

		// Sometimes the fileService is not created yet
		if (!fileService) {
// uncommented due to refactoring into a plugin
// and fileservice is gscf specific
// TODO --> fix
//			fileService = new FileService();
		}

		// Magic setter for files: handle values for file fields
		//
		// If NULL is given or "*deleted*", the field value is emptied and the old file is removed
		// If an empty string is given, the field value is kept as was
		// If a file is given, it is moved to the right directory. Old files are deleted. If
		//   the file does not exist, the field is kept
		// If a string is given, it is supposed to be a file in the upload directory. If
		//   it is different from the old one, the old one is deleted. If the file does not
		//   exist, the old one is kept.
		if (field.type == TemplateFieldType.FILE) {
			def currentFile = getFieldValue(field.name);

			if (value == null || ( value.class == String && value == '*deleted*' ) ) {
				// If NULL is given, the field value is emptied and the old file is removed
				value = "";
				if (currentFile) {
					fileService.delete(currentFile)
				}
			} else if (value.class == File) {
				// a file was given. Attempt to move it to the upload directory, and
				// afterwards, store the filename. If the file doesn't exist
				// or can't be moved, "" is returned
				value = fileService.moveFileToUploadDir(value);

				if (value) {
					if (currentFile) {
						fileService.delete(currentFile)
					}
				} else {
					value = currentFile;
				}
			} else if (value == "") {
				value = currentFile;
			} else {
				if (value != currentFile) {
					if (fileService.fileExists(value)) {
						// When a FILE field is filled, and a new file is set
						// the existing file should be deleted
						if (currentFile) {
							fileService.delete(currentFile)
						}
					} else {
						// If the file does not exist, the field is kept
						value = currentFile;
					}
				}
			}
		}

		// Magic setter for ontology terms: handle string values
		if (field.type == TemplateFieldType.ONTOLOGYTERM && value && value.class == String) {
			// iterate through ontologies and find term
			field.ontologies.each() { ontology ->
				// If we've found a term already, value.class == Term. In that case,
				// we shouldn't look further. Unfortunately, groovy doesn't support breaking out of
				// each(), so we check it on every iteration.
				if( value.class == String ) {
					def term = ontology.giveTermByName(value)

					// found a term?
					if (term) {
						value = term
					}
				}
			}

			// If the term is not found in any ontology
			if( value.class == String ) {
				// TODO: search ontology for the term online (it may still exist) and insert it into the Term cache
				// if not found, throw exception
				throw new IllegalArgumentException("Ontology term not recognized (not in the ontology cache): ${value} when setting field ${fieldName}")
			}
		}

		// Magic setter for TEMPLATE fields
		if (field.type == TemplateFieldType.TEMPLATE && value && value.class == String) {
			value = Template.findByName(value)
		}

		// Magic setter for MODULE fields
		if (field.type == TemplateFieldType.MODULE && value && value.class == String) {
			value = AssayModule.findByName(value)
		}

		// Magic setter for LONG fields
		if (field.type == TemplateFieldType.LONG && value && value.class == String) {
			// A check for invalidity is done in the validator of these fields. For that
			// reason, we just try to parse it here. If it fails, the validator will also
			// fail.
			try {
				value = Long.parseLong(value.trim())
			} catch( Exception e ) {}
		}

		// Set the field value
		if (isDomainField(field)) {
			// got a value?
			if (value) {
				//log.debug ".setting [" + ((super) ? super.class : '??') + "] ("+getIdentifier()+") domain field: [" + fieldName + "] ([" + value.toString() + "] of type [" + value.class + "])"
				this[field.name] = value
			} else {
				//log.debug ".unsetting [" + ((super) ? super.class : '??') + "] ("+getIdentifier()+") domain field: [" + fieldName + "]"

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
				//log.debug ".setting [" + ((super) ? super.class : '??') + "] ("+getIdentifier()+") template field: [" + fieldName + "] ([" + value.toString() + "] of type [" + value.class + "])"

				// set value
				store[fieldName] = value
			} else if (store[fieldName]) {
				//log.debug ".unsetting [" + ((super) ? super.class : '??') + "] ("+getIdentifier()+") template field: [" + fieldName + "]"

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