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
 *  Author	: Jeroen Wesbeek <work@osx.eu>
 *  Since	: 20110211
 *
 *  Revision Information:
 *  $Author$
 *  $Rev$
 *  $Date$
 */

package org.dbnp.gdt.ast
import org.codehaus.groovy.transform.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.ast.stmt.*
import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log

/**
 * Hook in the Groovy compiler and dynamically extend TemplateEntity with
 * all TemplateEntity fields (= classes) in the application (either in the
 * GDT plugin, other plugins or the application itself)
 *
 * As this is done before compilitation to byteCode, test this with:
 * clear;grails clean;grails compile
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class TemplateEntityASTTransformation implements ASTTransformation {
	static debug			= false
	static templateEntity	= null
	static templateFields	= []
	static otherClasses		= []
	private static final Log log = LogFactory.getLog(TemplateEntityASTTransformation.class)

	/**
	 * class constructor
	 * @return
	 */
	public TemplateEntityASTTransformation() {
		super
	}

	/**
	 * callback method which is called by the compiler when
	 * a Abstact Syntax Tree Node is visited
	 * @param nodes
	 * @param sourceUnit
	 */
	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		def classLoader = sourceUnit.getClassLoader()

		// iterate through the nodes
		nodes.each { node ->
			// find all TemplateFields or TemplateEntity nodes
			node.getClasses().findAll{it.name =~ /\.Template([A-Za-z]{1,})Field$/ || it.name =~ /\.TemplateEntity$/}.each { ClassNode owner ->
				// is this TemplateEntity or a TemplateField?
				if (owner.name =~ /\.TemplateEntity$/) {
					// remember templateEntity so we can inject
					// templatefields into it
					templateEntity = owner
					return
				} else if (templateEntity) {
					// inject this template field into TemplateEntity
					injectTemplateField(templateEntity, owner, classLoader)

					// got some cached template fields to handle?
					if (templateFields.size()) {
						templateFields.each {
							injectTemplateField(it, owner, classLoader)
						}
						templateFields = []
					}
				} else {
					// cache this templateField until the
					// compilere visits TemplateEntity
					templateFields[ templateFields.size() ] = owner
					return
				}
			}

			// cache the other classes we visit as we may need
			// them later when dynamically extending hasMany
			// relationships
			// @see TemplateEntityASTTransformation.extendHasMany
			node.getClasses().findAll{!(it.name =~ /\.Template([A-Za-z]{1,})Field$/ || it.name =~ /\.TemplateEntity$/)}.each { ClassNode owner ->
				otherClasses[ otherClasses.size() ] = owner
			}
		}
	}

	/**
	 * add a template field to templateEntity
	 * @param templateEntityClassNode
	 * @param templateFieldClassNode
	 * @param classLoader
	 */
	private void injectTemplateField(ClassNode templateEntityClassNode, ClassNode templateFieldClassNode, classLoader) {
		def contains 				= templateFieldClassNode.fields.find { it.properties.name == "contains" }
		def owner					= contains.properties.owner
		def typeName				= contains.getInitialExpression().variable
		def splitTemplateFieldName	= owner.name.split("\\.")
		def templateFieldName		= splitTemplateFieldName[(splitTemplateFieldName.size() - 1)]

		// debug
		if (debug) println "injecting ${templateFieldClassNode} into ${templateEntityClassNode}"

		// todo: refactor TemplateOntologyTermField into TemplateTermField so it is consistent with the rest
		//		 for now sticking to the replaceAll to keep this stupid exception to the rule working...
		def templateFieldMapName	= (templateFieldName[0].toLowerCase() + templateFieldName.substring(1) + "s").replaceAll(/Ontology/, '')

		// a GORM Map for this templateField to TemplateEntity, e.g.:
		//
		// Map templateLongFields = [:]
		addTemplateFieldMap(templateEntityClassNode, templateFieldMapName)

		// extend the hasMany relationship in TemplateEntity, e.g:
		//
		// static hasMany = [
		// 		...
		//		templateLongFields: long,
		//		...
		// ]
		extendHasMany(templateEntityClassNode, templateFieldName, templateFieldMapName, typeName)

		// extend the TemplateEntity constraints and inject a dynamic
		// validator, e.g.:
		//
		// static constraints = {
		//		...
		//		templateLongFields(validator: TemplateLongField.validator)
		//		...
		// }
		extendConstraints(templateEntityClassNode, templateFieldClassNode, templateFieldMapName, templateFieldName)
	}

	/**
	 * Extend the TemplateEntity constraints and dynamically inject a validator
	 * for the given TemplateField. In groovy code this would be:
	 *
	 * static constraints = {
	 *     ...
	 *     templateLongFields(validator: TemplateLongField.validator)
	 *     ...
	 * }
	 *
	 * @param templateEntityClassNode
	 * @param templateFieldClassNode
	 * @param templateFieldMapName
	 * @param templateFieldName
	 * @return
	 */
	public extendConstraints(ClassNode templateEntityClassNode, ClassNode templateFieldClassNode, String templateFieldMapName, String templateFieldName) {
		// see http://svn.codehaus.org/grails-plugins/grails-burning-image/trunk/src/java/pl/burningice/plugins/image/ast/AbstractImageContainerTransformation.java
		if (GrailsASTUtils.hasProperty(templateEntityClassNode, "constraints")) {
			FieldNode constraints = templateEntityClassNode.getDeclaredField("constraints")

			if (hasFieldInClosure(constraints, templateFieldMapName)) {
				//println " - constraint closure already exists, skip this"
			} else {
				// debug
				if (debug) println " - extending constraints closure"

				// get the constraints closure
				ClosureExpression initialExpression = (ClosureExpression) constraints.getInitialExpression();
				BlockStatement blockStatement		= (BlockStatement) initialExpression.getCode();

				// create the validator property
				PropertyExpression validator = new PropertyExpression(
					new VariableExpression(templateFieldName),
					"validator"
				)

				// create expression arguments
				NamedArgumentListExpression arguments = new NamedArgumentListExpression();
                arguments.addMapEntryExpression(
					new ConstantExpression("validator"),			// validator key (name)
					validator										// validator expression
				)

				// create expression using the expression arguments
				MethodCallExpression constantExpression = new MethodCallExpression(
					VariableExpression.THIS_EXPRESSION,				// object
					new ConstantExpression(templateFieldMapName),	// method
					arguments										// arguments
				)

				// add the newly created expression to the contraints' initialExpression
				blockStatement.addStatement(new ExpressionStatement(constantExpression))
			}
		} else {
			throw new Exception('This should not happen! This means that TemplateEntity does not have a \'constraints\' closure, which it is expected to have!')
		}
	}

	/**
	 * add template field map to TemplateEntity
	 * @param templateEntityClassNode
	 * @param templateFieldMapName
	 * @return
	 */
	public addTemplateFieldMap(ClassNode templateEntityClassNode, String templateFieldMapName) {
		if (!GrailsASTUtils.hasProperty(templateEntityClassNode, templateFieldMapName)) {
		    // debug
			if (debug) println " - adding ${templateFieldMapName} map..."

			templateEntityClassNode.addProperty(templateFieldMapName, Modifier.PUBLIC, new ClassNode(java.util.Map.class), new MapExpression(), null, null)
		} else {
			// debug
			if (debug) println " - ${templateFieldMapName} Map is already present, skip this"
		}
	}

	/**
	 * add hasMany relation ship
	 * @param templateEntityClassNode
	 * @param templateFieldName
	 * @param templateFieldMapName
	 * @param typeName
	 * @return
	 */
	public extendHasMany(ClassNode templateEntityClassNode, String templateFieldName, String templateFieldMapName, String typeName) {
		// is it a build in class?
		def myClass
		def words
		try {
			// try to use a build in class
			myClass = new ClassNode(Eval.me("${typeName}.class"))
		} catch (Exception e) {
			// no, use class cache which contains
			// all currently visited classes, and
			// contains ClassNodes for each class
			myClass = otherClasses.find {
				words = it.name.split("\\.")
				return (words[ (words.size()-1) ] == typeName)
			}
		}

		// does templateEntity contain a hasMany map?
		if (GrailsASTUtils.hasProperty(templateEntityClassNode, "hasMany")) {
			PropertyNode hasMany = templateEntityClassNode.getProperty("hasMany")
			MapExpression initialExpression = hasMany.getInitialExpression()
			if (!initialExpression.getMapEntryExpressions().find{it.getKeyExpression().toString() =~ templateFieldMapName}) {
				// debug
				if (debug) println " - extending hasMany map"

				initialExpression.addMapEntryExpression(new ConstantExpression(templateFieldMapName), new ClassExpression(myClass))
				//initialExpression.addMapEntryExpression(new ConstantExpression(templateFieldMapName), new VariableExpression(myClass))
			} else {
				// debug
				if (debug) println " - hasMany map entry for ${templateFieldName} already present"
			}

			// show all hasMany entries
			if (debug) hasMany.getInitialExpression().properties.mapEntryExpressions.each { println it }
		} else {
			// debug
			if (debug) println " - adding hasMany map"

			MapExpression initialExpression = new MapExpression()
			initialExpression.addMapEntryExpression(new ConstantExpression(templateFieldMapName), new ClassExpression(myClass))
			templateEntityClassNode.addProperty("hasMany", Modifier.PUBLIC | Modifier.STATIC, new ClassNode(java.util.Map.class), initialExpression, null, null)
		}
	}

	/**
	 * check if a field exists in a closure
	 * @param closure
	 * @param fieldName
	 * @return
	 */
	public boolean hasFieldInClosure(FieldNode closure, String fieldName){
		if(closure != null){
			ClosureExpression initialExpression	= (ClosureExpression) closure.getInitialExpression()
			BlockStatement blockStatement		= (BlockStatement) initialExpression.getCode()
			List<Statement> statements			= blockStatement.getStatements()

			// iterate through block statements
			for(Statement expressionStatement : statements){
				// does the expression statement contain a method?
				if(expressionStatement instanceof ExpressionStatement && ((ExpressionStatement)expressionStatement).getExpression() instanceof MethodCallExpression){
					// yes, get the expression
					MethodCallExpression methodCallExpression = (MethodCallExpression)((ExpressionStatement)expressionStatement).getExpression()

					// get the method
					ConstantExpression constantExpression = (ConstantExpression)methodCallExpression.getMethod()

					// is it the same as the field name?
					if(constantExpression.getValue().equals(fieldName)){
						return true
					}
				}
			}
		}
		return false
	}
}