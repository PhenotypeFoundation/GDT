package org.dbnp.gdt.ast

import org.codehaus.groovy.transform.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.ast.stmt.*
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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

				ClosureExpression initialExpression = (ClosureExpression) constraints.getInitialExpression();
				BlockStatement blockStatement = (BlockStatement) initialExpression.getCode();

				//templateTermFields(validator		: TemplateOntologyTermField.validator)
/*
				StaticMethodCallExpression closureMethodCall = new StaticMethodCallExpression(
					templateFieldClassNode,
					"validator",
					new ArgumentListExpression(
						//(VariableExpression)image,
						//(VariableExpression)imageContainer
					)
				);
				BlockStatement closureBody = new BlockStatement(
					{
						new ReturnStatement(closureMethodCall)
					},
					new VariableScope()
				);


				Parameter[] closureParameters = {
					//new Parameter(new ClassNode(MultipartFile.class), "image"),
					//new Parameter(new ClassNode(ImageContainer.class), "imageContainer")
				};

				//VariableScope scope = new VariableScope();
				//scope.putDeclaredVariable(image);
				//scope.putDeclaredVariable(imageContainer);

				ClosureExpression validator = new ClosureExpression(closureParameters, closureBody);
				//validator.setVariableScope(scope);

				NamedArgumentListExpression namedArgumentListExpression = new NamedArgumentListExpression();
				namedArgumentListExpression.addMapEntryExpression(new ConstantExpression("validator"), validator);

				MethodCallExpression constantExpression = new MethodCallExpression(
					VariableExpression.THIS_EXPRESSION,
					new ConstantExpression(templateFieldMapName),
					namedArgumentListExpression
				);

				// add new validator to the blockstatement
				blockStatement.addStatement(new ExpressionStatement(constantExpression));



				/*
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

				 */

				/*

				ClosureExpression initialExpression	= (ClosureExpression) constraints.getInitialExpression();
            	BlockStatement blockStatement		= (BlockStatement) initialExpression.getCode();

				//templateTermFields(validator		: TemplateOntologyTermField.validator)
				StaticMethodCallExpression closureMethodCall = new StaticMethodCallExpression(
					new ClassNode(Eval.me("${templateFieldName}.class")),
						"validator",
						new ArgumentListExpression(
							//(VariableExpression)image,
							//(VariableExpression)imageContainer
						)
				);
				BlockStatement closureBody = new BlockStatement(
					new Statement[]{
						new ReturnStatement(closureMethodCall)
					},
					new VariableScope()
				);


 Parameter[] closureParameters = {new Parameter(new ClassNode(MultipartFile.class), "image"),
                                                 new Parameter(new ClassNode(ImageContainer.class), "imageContainer")};

                VariableScope scope = new VariableScope();
                scope.putDeclaredVariable(image);
                scope.putDeclaredVariable(imageContainer);

                ClosureExpression validator = new ClosureExpression(closureParameters, closureBody);
                validator.setVariableScope(scope);

                NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
                namedarg.addMapEntryExpression(new ConstantExpression("validator"), validator);

                MethodCallExpression constExpr = new MethodCallExpression(VariableExpression.THIS_EXPRESSION,
                                                                          new ConstantExpression(fieldName),
                                                                          namedarg);
                block.addStatement(new ExpressionStatement(constExpr));
                */



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
println "\n----"
println expressionStatement
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