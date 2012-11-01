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

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.apache.commons.lang.RandomStringUtils

class GdtService implements Serializable {
    // Must be false, since the webflow can't use a transactional service. See
    // http://www.grails.org/WebFlow for more information
    static transactional = false

	// cached crypto secret
	static cachedSecret

	// cached template entities
	static cachedEntities

	/**
	 * return the templateField class based on casedName
	 * @param casedName
	 * @return
	 */
	def public getTemplateFieldTypeByCasedName(String casedName) {
		def grailsApplication = ApplicationHolder.application
		return grailsApplication.getAllClasses().find{it.name =~ "${casedName}Field" && it.name =~ /Template([A-Za-z]{1,})Field$/}
	}

	/**
	 * get all domain classes that use the domain templates
	 * @return map
	 */
	def getTemplateEntities() {
		// return cached entities if present
		if (cachedEntities) return cachedEntities

		// fetch entities and cache them
		def grailsApplication = ApplicationHolder.application
		def entities = []

		// iterate through domain classes
		grailsApplication.getArtefacts("Domain").each {
			def myInstance = it.clazz
			if (myInstance.properties.superclass.toString() =~ 'TemplateEntity') {
				def matches	= myInstance.toString() =~ /\.([^\.]+)$/

				entities[entities.size()] = [
					name		: matches[0][1],
					description	: matches[0][1].replaceAll(/([A-Z])/, ' $1').replaceFirst(/^ /,''),
					entity		: myInstance.toString().replaceFirst(/^class /,''),
					instance	: myInstance,
					encoded		: encryptEntity(myInstance.toString())
				]
			}
		}

		// cache entities
		cachedEntities = entities

		return cachedEntities
	}

	/**
	 * return the crypto secret
	 *
	 * fetches the pre-configured secret, or generates
	 * a random secret on the fly
	 *
	 * For a static secret (works perhaps better in multi
	 * server/ loadbalanced environments) add the following
	 * to your Config.groovy:
	 *
	 * crypto {
	 * 	shared.secret = "yourSecretCanBeInHere"
	 * }
	 *
	 * @return String
	 */
	private getSecret() {
		// do we have a static secret?
		if (cachedSecret) return cachedSecret

		// is the secret in the configuration?
		def grailsApplication = ApplicationHolder.application
		if (!grailsApplication.config.crypto) {
			// we have not secret, generate a random secret
			grailsApplication.config.crypto.shared.secret = RandomStringUtils.random(32, true, true)
		}

		// set static secret
		cachedSecret = grailsApplication.config.crypto.shared.secret

		// and return the static secret
		return cachedSecret
	}

	/**
	 * encrypt the name of an entity
	 * @param String entityName
	 * @return String
	 */
	def String encryptEntity(String entityName) {
		// generate a Blowfish encrypted and Base64 encoded string
		/*return URLEncoder.encode(
			Blowfish.encryptBase64(
				entityName.replaceAll(/^class /, ''),
				getSecret()
			)
		) */
		entityName.replaceAll(/^class /, '')
	}

	/**
	 * decrypt an entity
	 * @param String entity
	 * @return String
	 */
	def String decryptEntity(String entity) {
		// generate a Blowfish decrypted and Base64 decoded string.
		//return Blowfish.decryptBase64(entity, getSecret())
		entity
	}

	/**
	 * instantiate by encrypted entity
	 * @param String entity
	 * @return Object
	 */
	def getInstanceByEntity(String entity) {
		return getInstanceByEntityName(decryptEntity(entity))
	}

	/**
	 * instantiate by entity name
	 * @param String entityName
	 * @return Object
	 */
	def getInstanceByEntityName(String entityName) {
		def grailsApplication = ApplicationHolder.application
		def entity

		// dynamically instantiate the entity (if possible)
		try {
			entity = Class.forName(entityName, true, grailsApplication.getClassLoader())

			// succes, is entity an instance of TemplateEntity?
			if (entity && entity.superclass =~ /TemplateEntity$/ || entity.superclass.superclass =~ /TemplateEntity$/) {
				return entity
			}
		} catch (Exception e) {}

		return false
	}

	/**
	 * check if an entity is valid
	 * @param entity
	 * @return
	 */
	def Boolean checkEntity(String entity) {
		if (getInstanceByEntity(entity)) {
			return true
		} else {
			return false
		}
	}
}