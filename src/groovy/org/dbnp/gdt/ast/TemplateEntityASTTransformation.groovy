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
		final boolean hasHasMany = GrailsASTUtils.hasProperty(templateEntityClassNode, "hasMany");
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
		extendConstraints(templateEntityClassNode, templateFieldMapName)
	}

	public extendConstraints(ClassNode templateEntityClassNode, String templateFieldMapName) {
		// see http://svn.codehaus.org/grails-plugins/grails-burning-image/trunk/src/java/pl/burningice/plugins/image/ast/AbstractImageContainerTransformation.java


		if (GrailsASTUtils.hasProperty(templateEntityClassNode, "constraints")) {
			FieldNode constraints = templateEntityClassNode.getDeclaredField("constraints");

			if (hasFieldInClosure(constraints, templateFieldMapName)) {
				println " - constraint closure already exists"
			} else {
				println " - extending constraints closure"
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
			ClosureExpression exp = (ClosureExpression) closure.getInitialExpression()
			BlockStatement block = (BlockStatement) exp.getCode()
			List<Statement> ments = block.getStatements()
			for(Statement expstat : ments){
				if(expstat instanceof ExpressionStatement && ((ExpressionStatement)expstat).getExpression() instanceof MethodCallExpression){
					MethodCallExpression methexp = (MethodCallExpression)((ExpressionStatement)expstat).getExpression()
					ConstantExpression conexp = (ConstantExpression)methexp.getMethod()
					if(conexp.getValue().equals(fieldName)){
						return true
					}
				}
			}
		}
		return false
	}
}