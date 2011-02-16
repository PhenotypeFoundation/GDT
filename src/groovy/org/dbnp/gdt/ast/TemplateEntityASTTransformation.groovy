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
import org.springframework.web.multipart.MultipartFile

// Hook in Groovy compilation and transform the
// Abstract Syntax Tree (ATS)

@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class TemplateEntityASTTransformation implements ASTTransformation {
	static templateEntity = null
	private static final Log LOG = LogFactory.getLog(TemplateEntityASTTransformation.class)

	/**
	 * class constructor
	 * @return
	 */
	public TemplateEntityASTTransformation() {
		super
	}

	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		def classLoader = sourceUnit.getClassLoader()

		nodes.each { node ->
			node.getClasses().findAll{it.name =~ /\.Template([A-Za-z]{1,})Field$/ || it.name =~ /\.TemplateEntity$/}.each { ClassNode owner ->
				// remember templateEntity
				if (owner.name =~ /\.TemplateEntity$/) {
					templateEntity = owner
					return
				} else if (templateEntity) {
					injectTemplateField(templateEntity, owner, classLoader)
				}
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
		println "injecting ${templateFieldClassNode} into ${templateEntityClassNode}"
		def contains 				= templateFieldClassNode.fields.find { it.properties.name == "contains" }
		def owner					= contains.properties.owner
		def typeName				= contains.getInitialExpression().variable
		def splitTemplateFieldName	= owner.name.split("\\.")
		def templateFieldName		= splitTemplateFieldName[(splitTemplateFieldName.size() - 1)]
		def templateFieldMapName	= (templateFieldName[0].toLowerCase() + templateFieldName.substring(1) + "s").replaceAll(/Ontology/, '')

		// add map to templateEntity
		addTemplateFieldMap(templateEntityClassNode, templateFieldMapName)

		// extend hasMany
		extendHasMany(templateEntityClassNode, templateFieldName, templateFieldMapName, typeName)

		// extend constraints
		extendConstraints(templateEntityClassNode, templateFieldClassNode, templateFieldMapName, templateFieldName)
	}

	public extendConstraints(ClassNode templateEntityClassNode, ClassNode templateFieldClassNode, String templateFieldMapName, String templateFieldName) {
		// see http://svn.codehaus.org/grails-plugins/grails-burning-image/trunk/src/java/pl/burningice/plugins/image/ast/AbstractImageContainerTransformation.java
		if (GrailsASTUtils.hasProperty(templateEntityClassNode, "constraints")) {
			FieldNode constraints = templateEntityClassNode.getDeclaredField("constraints")

			if (hasFieldInClosure(constraints, templateFieldMapName)) {
				println " - constraint closure already exists"
			} else {
				println " - extending constraints closure"

println "1"
				ClosureExpression initialExpression = (ClosureExpression) constraints.getInitialExpression();
println "2"
				BlockStatement blockStatement		= (BlockStatement) initialExpression.getCode();

				/*
//templateTermFields(validator		: TemplateOntologyTermField.validator)

org.codehaus.groovy.ast.stmt.ExpressionStatement@1148ab5c[
	expression:org.codehaus.groovy.ast.expr.MethodCallExpression@39ea2de1[
		object: org.codehaus.groovy.ast.expr.VariableExpression@72433b8a[variable: this]
		method: ConstantExpression[templateTermFields]
		arguments: org.codehaus.groovy.ast.expr.TupleExpression@3d6a2c7b[
			org.codehaus.groovy.ast.expr.NamedArgumentListExpression@58e5ebd[
				org.codehaus.groovy.ast.expr.MapEntryExpression@45edcd24(
					key: ConstantExpression[validator],
					value: org.codehaus.groovy.ast.expr.PropertyExpression@7f371a59[
						object: org.codehaus.groovy.ast.expr.VariableExpression@7aa30a4e[
							variable: TemplateOntologyTermField
						]
						property: ConstantExpression[validator]
					]
				)
			]
		]
	]
]

newly created statement:
org.codehaus.groovy.ast.expr.MethodCallExpression@1a06f956[
	object: org.codehaus.groovy.ast.expr.VariableExpression@3fdb8a73[variable: this]
	method: ConstantExpression[templateLongFields]
	arguments: org.codehaus.groovy.ast.expr.TupleExpression@665ea4c5[
		org.codehaus.groovy.ast.expr.NamedArgumentListExpression@4f93b604[
			org.codehaus.groovy.ast.expr.MapEntryExpression@6a92e96c(
				key: ConstantExpression[validator],
				value: org.codehaus.groovy.ast.expr.ClosureExpression@531ae81d[]{
					org.codehaus.groovy.ast.stmt.ExpressionStatement@b7cf28b[
						expression:org.codehaus.groovy.ast.expr.PropertyExpression@38178991[
							object: org.codehaus.groovy.ast.expr.VariableExpression@1148ab5c[variable: it]
							property: ConstantExpression[class]
						]
					]
				}
			)
		]
	]
]

				*/
println "3a"
				ExpressionStatement expression = new ExpressionStatement(
						new PropertyExpression(
							new VariableExpression("it"),
							"class"
						)
					)
println "3b"
//Parameter param = new Parameter(new ClassNode(), "$it")
//Parameter param = new Parameter(new ClassNode(MultipartFile.class), "image")

/*
                def param = [
					new Parameter(
                        ClassHelper.make(Object, false), "parm"
                	)
				] as Parameter[]
*/
println "3c"
				ClosureExpression validator = new ClosureExpression(
					[] as Parameter[],
					expression
				)

				/*
ClosureExpression closureExpression = new ClosureExpression (
new Parameter[] {}
new ExpressionStatement(new PropertyExpression(new VariableExpression("it"), "class"))
);
				 */

				// create expression arguments
println "3d"
				NamedArgumentListExpression arguments = new NamedArgumentListExpression();
println "4"
                arguments.addMapEntryExpression(
					new ConstantExpression("validator"),			// validator key (name)
					validator										// validator expression
				);
println "5"

				// create expression using the expression arguments
				MethodCallExpression constantExpression = new MethodCallExpression(
					VariableExpression.THIS_EXPRESSION,				// object
					new ConstantExpression(templateFieldMapName),	// method
					arguments										// arguments
				);

				println "newly created statement:"
				println constantExpression


				// add the newly created expression to the contraints' initialExpression
				blockStatement.addStatement(new ExpressionStatement(constantExpression))

			}
		} else {
			println " - adding constraints closure"
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
		    println " - adding ${templateFieldMapName}..."
			templateEntityClassNode.addProperty(templateFieldMapName, Modifier.PUBLIC, new ClassNode(java.util.Map.class), new MapExpression(), null, null)
		} else {
			println " - ${templateFieldMapName} Map is already present"
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
		try {
			def myClass = Eval.me("${typeName}.class")
		} catch (Exception e) {
			println " - !!! COULD NOT LOAD ${typeName}"
		}

		if (GrailsASTUtils.hasProperty(templateEntityClassNode, "hasMany")) {
			PropertyNode hasMany = templateEntityClassNode.getProperty("hasMany")
			MapExpression initialExpression = hasMany.getInitialExpression()
			if (!initialExpression.getMapEntryExpressions().find{it.getKeyExpression().toString() =~ templateFieldMapName}) {
				println " - extending hasMany map"
				initialExpression.addMapEntryExpression(new ConstantExpression(templateFieldMapName), new ClassExpression(new ClassNode(Eval.me("${typeName}.class"))))
			} else {
				println " - hasMany map entry for ${templateFieldName} already present"
			}
		} else {
			println " - adding hasMany map"
			MapExpression initialExpression = new MapExpression()
			initialExpression.addMapEntryExpression(new ConstantExpression(templateFieldMapName), new ClassExpression(new ClassNode(Eval.me("${typeName}.class"))))
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
//println "\n----"
//println expressionStatement
					// yes, get the expression
					MethodCallExpression methodCallExpression	= (MethodCallExpression)((ExpressionStatement)expressionStatement).getExpression()

					// get the method
					ConstantExpression constantExpression		= (ConstantExpression)methodCallExpression.getMethod()

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