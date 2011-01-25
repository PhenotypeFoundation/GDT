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
package nl.grails.plugins.gdt
import org.springframework.beans.factory.InitializingBean
import cr.co.arquetipos.crypto.Blowfish

class GdtService implements InitializingBean {
	def grailsApplication
	def setting

	void afterPropertiesSet() {
		this.setting = grailsApplication.config.setting
	}

	/**
	 * get all domain classes that use the domain templates
	 * @return map
	 */
	def getTemplateEntities() {
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

		return entities
	}

	/**
	 * encrypt the name of an entity
	 * @param String entityName
	 * @return String
	 */
	def String encryptEntity(String entityName) {
		if (grailsApplication.config.crypto) {
			// generate a Blowfish encrypted and Base64 encoded string
			return URLEncoder.encode(
				Blowfish.encryptBase64(
					entityName.replaceAll(/^class /, ''),
					grailsApplication.config.crypto.shared.secret
				)
			)
		} else {
			// base64 only; this is INSECURE! Even though it is not
			// very likely, it is possible to exploit this and have
			// Grails dynamically instantiate whatever class you like.
			// If that constructor does something harmfull this could
			// be dangerous. Hence, use encryption (above) instead...
			return URLEncoder.encode(entityName.replaceAll(/^class /, '').bytes.encodeBase64())
		}
		return entityName
	}

	/**
	 * decrypt an entity
	 * @param String entity
	 * @return String
	 */
	def String decryptEntity(String entity) {
		def entityName

		if (grailsApplication.config.crypto) {
			// generate a Blowfish decrypted and Base64 decoded string.
			entityName = Blowfish.decryptBase64(
				entity,
				grailsApplication.config.crypto.shared.secret
			)
		} else {
			entityName = new String(entity.toString().decodeBase64())
		}

		return entityName
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