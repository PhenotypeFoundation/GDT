package org.dbnp.gdt.ast

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.transform.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import java.lang.reflect.Modifier

/*
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.PropertyNode
import org.apache.commons.lang.StringUtils
import java.lang.reflect.Modifier
*/

// Hook in Groovy compilation and transform the
// Abstract Syntax Tree (ATS)

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class TemplateEntityASTTransformation implements ASTTransformation {
	static templateEntity = null
	static MAP_TYPE = new ClassNode(Map)

	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		nodes.each { node ->
			node.getClasses().findAll{it.name =~ /\.Template([A-Za-z]{1,})Field$/ || it.name =~ /\.TemplateEntity$/}.each { ClassNode owner ->
				if (owner.name =~ /\.TemplateEntity$/) {
					templateEntity = owner
					return
				}

				if (templateEntity) {
					injectTemplateField(templateEntity, owner)
				}
			}
		}
	}

	private void injectTemplateField(ClassNode templateEntityClassNode, ClassNode templateFieldClassNode) {
		println "injecting ${templateFieldClassNode} into ${templateEntityClassNode}"

/*
		def splitTemplateFieldName	= templateFieldClassNode.name.split("\\.")
		def templateFieldName		= splitTemplateFieldName[(splitTemplateFieldName.size() - 1)]
		def templateFieldMapName	= templateFieldName[0].toLowerCase() + templateFieldName.substring(1) + "s"
		def templateFieldType		= getProperty(templateFieldClassNode, 'contains').type
		def templateFieldTypeName	= templateFieldType.toString().replaceAll(/ (.*)$/, '')

		println "1. "+templateFieldClassNode.class
		println "2. "+templateFieldTypeName
//		println "3. "+Class.forName(templateFieldTypeName)
		println "4. "+getProperty(templateFieldClassNode, 'contains')
		println "5. "+getProperty(templateFieldClassNode, 'contains').type
		println "6. "+getProperty(templateFieldClassNode, 'contains').class
		println "7. "+templateFieldClassNode.getDeclaredField("contains")
		println "8."+templateFieldClassNode.getDeclaredField("contains").properties.originType
		println "9."+templateFieldClassNode.getDeclaredField("contains").properties.originType.class
		println "10."+templateFieldClassNode.getDeclaredField("contains").properties.initialExpression
		println "11."+templateFieldClassNode.getDeclaredField("contains").initialExpression
		println "12."+templateFieldClassNode.getDeclaredField("contains").initialExpression.type
		//println "12"+templateFieldClassNode.getDeclaredField("contains").initialExpression.getField(type)

		templateFieldClassNode.getDeclaredField("contains").initialExpression.each {
			println it
		}
*/


		// inject templateFieldMap?
		//injectTemplateFieldMap(templateEntityClassNode, templateFieldMapName)
		//injectHasMany(templateEntityClassNode, templateFieldMapName, templateFieldClassNode.getDeclaredField("contains").initialExpression.type)
		//injectConstraint(templateEntityClassNode, templateFieldMapName)
	}

	private void injectTemplateFieldMap(ClassNode templateEntityClassNode, String templateFieldName) {
		PropertyNode templateFieldMap = getProperty(templateEntityClassNode, templateFieldName)

		// check if map exists
		if (!templateFieldMap) {
			// no, inject it
			println " - injecting templateFieldMap ${templateFieldName} into ${templateEntityClassNode.name}"
			def newTemplateFieldMap = templateEntityClassNode.addProperty(templateFieldName, Modifier.PUBLIC, MAP_TYPE, null, null, null)
		} else if (templateFieldMap.type.typeClass != Map.class) {
			// yes, but it is not a map
			println " - changing templateFieldMap ${templateFieldName} to type Map"
			templateFieldMap.field.type = MAP_TYPE
		}
	}

	private void injectHasMany(ClassNode templateEntityClassNode, String templateFieldName, templateFieldType) {
		println "-inject hasMany ${templateFieldName}"
		FieldNode hasMany			= getHasManyField(templateEntityClassNode)

		//BlockStatement statement	= expression.code

		println hasMany
		hasMany.properties.each {
			println it
		}

		/*
initialValueExpression=org.codehaus.groovy.ast.expr.MapExpression@11d612fc
[
org.codehaus.groovy.ast.expr.MapEntryExpression@7d786789(
 key: ConstantExpression[templateStringFields], value: org.codehaus.groovy.ast.expr.ClassExpression@6bd593b9[type: java.lang.String]),
 org.codehaus.groovy.ast.expr.MapEntryExpression@12d58dfe(key: ConstantExpression[templateTextFields], value: org.codehaus.groovy.ast.expr.ClassExpression@25d285b[type: java.lang.String]), org.codehaus.groovy.ast.expr.MapEntryExpression@32046f93(key: ConstantExpression[templateStringListFields], value: org.codehaus.groovy.ast.expr.ClassExpression@4139358c[type: org.dbnp.gdt.TemplateFieldListItem]), org.codehaus.groovy.ast.expr.MapEntryExpression@12ea9bc8(key: ConstantExpression[templateDoubleFields], value: org.codehaus.groovy.ast.expr.ClassExpression@6899712b[type: double]), org.codehaus.groovy.ast.expr.MapEntryExpression@31d4f3b3(key: ConstantExpression[templateDateFields], value: org.codehaus.groovy.ast.expr.ClassExpression@13cb8654[type: java.util.Date]), org.codehaus.groovy.ast.expr.MapEntryExpression@16dffef3(key: ConstantExpression[templateTermFields], value: org.codehaus.groovy.ast.expr.ClassExpression@4bdb0f40[type: org.dbnp.bgdt.Term]), org.codehaus.groovy.ast.expr.MapEntryExpression@1ac659a8(key: ConstantExpression[templateRelTimeFields], value: org.codehaus.groovy.ast.expr.ClassExpression@54283253[type: long]), org.codehaus.groovy.ast.expr.MapEntryExpression@1b00124f(key: ConstantExpression[templateFileFields], value: org.codehaus.groovy.ast.expr.ClassExpression@21b20e62[type: java.lang.String]), org.codehaus.groovy.ast.expr.MapEntryExpression@30ce5536(key: ConstantExpression[templateBooleanFields], value: org.codehaus.groovy.ast.expr.ClassExpression@3a3c6542[type: boolean]), org.codehaus.groovy.ast.expr.MapEntryExpression@531bedec(key: ConstantExpression[templateTemplateFields], value: org.codehaus.groovy.ast.expr.ClassExpression@4ea14b94[type: org.dbnp.gdt.Template]), org.codehaus.groovy.ast.expr.MapEntryExpression@3f6909e0(key: ConstantExpression[templateModuleFields], value: org.codehaus.groovy.ast.expr.ClassExpression@90771a6[type: org.dbnp.gdt.AssayModule]), org.codehaus.groovy.ast.expr.MapEntryExpression@47b64deb(key: ConstantExpression[templateLongFields], value: org.codehaus.groovy.ast.expr.ClassExpression@11b5a415[type: long]), org.codehaus.groovy.ast.expr.MapEntryExpression@2c2767c8(key: ConstantExpression[systemFields], value: org.codehaus.groovy.ast.expr.ClassExpression@4f0e921d[type: org.dbnp.gdt.TemplateField])]

		 */

		if (hasMany.hasInitialExpression()) {
			MapExpression expression	= hasMany.initialExpression
			// add
			println "jaaaa"
			println expression.type
			println expression
			//expression.addMapEntryExpression(new Expression(templateFieldName), new Expression(String))
			//expression.addMapEntryExpression(new ConstantExpression(templateFieldName),new ConstantExpression(String))
			//mapValues.addMapEntryExpression(new ConstantExpression("biImage"), new ClassExpression(new ClassNode(Image.class)));
			//expression.addMapEntryExpression(new ConstantExpression(templateFieldName),new ClassExpression(new ClassNode(String.class)))
			expression.addMapEntryExpression(new ConstantExpression(templateFieldName),new ClassExpression(new ClassNode(templateFieldType)))
		}


		/*
				assert p.hasInitialExpression()

		ClosureExpression expr = p.initialExpression
		BlockStatement st = expr.code;
		// add a nullable:true constraint for node
		st.statements.add(
			new ExpressionStatement(
				new MethodCallExpression(
					new VariableExpression("this"),
					"node",
					new NamedArgumentListExpression(
						[new MapEntryExpression(new ConstantExpression("nullable"), new ConstantExpression(true))]
					)
				)
			)
		)
		 */
	}

	private void injectConstraint(ClassNode templateEntityClassNode, String templateFieldName) {
		println "-inject constraint ${templateFieldName}"
		FieldNode constraints = getConstraintsField(templateEntityClassNode)
		ClosureExpression expr = constraints.initialExpression
		BlockStatement st = expr.code

		println constraints

	}

	private PropertyNode getProperty(ClassNode classNode, String propertyName) {
		if (classNode == null || StringUtils.isBlank(propertyName))
			return null

		// find the given class property
		// do we need to deal with parent classes???
		for (PropertyNode pn: classNode.properties) {
			if (pn.getName().equals(propertyName) && !pn.isPrivate()) {
				return pn
			}
		}

		return null
	}

    protected FieldNode getHasManyField(ClassNode node){
        FieldNode hasManyField = node.getDeclaredField("hasMany");

        if (hasManyField != null) {
            return hasManyField;
        }

        hasManyField = new FieldNode("hasMany",
                                     Modifier.PRIVATE | Modifier.STATIC,
                                     new ClassNode(Map.class),
                                     new ClassNode(node.getClass()),
                                     new MapExpression());

        node.addField(hasManyField);
        //addGetter(hasManyField, node, Modifier.PUBLIC | Modifier.STATIC);
        //addSetter(hasManyField, node, Modifier.PUBLIC | Modifier.STATIC);
        return hasManyField;
    }

    protected FieldNode getConstraintsField(ClassNode node){
        FieldNode constraintsField = node.getDeclaredField("contraints");

        if (constraintsField != null) {
            return constraintsField;
        }
    }

	/*

    protected void addGetter(FieldNode fieldNode, ClassNode owner) {
        addGetter(fieldNode.getName(), fieldNode, owner, ACC_PUBLIC);
    }

    protected void addGetter(String name, FieldNode fieldNode, ClassNode owner) {
        addGetter(name, fieldNode, owner, ACC_PUBLIC);
    }

    protected void addGetter(FieldNode fieldNode, ClassNode owner, int modifier) {
        addGetter(fieldNode.getName(), fieldNode, owner, modifier);
    }

    protected void addGetter(String name, FieldNode fieldNode, ClassNode owner, int modifier) {
        ClassNode type = fieldNode.getType();
        String getterName = "get" + StringUtils.capitalize(name);
        owner.addMethod(getterName,
                modifier,
                nonGeneric(type),
                Parameter.EMPTY_ARRAY,
                null,
                new ReturnStatement(new FieldExpression(fieldNode)));
    }

    protected void addSetter(FieldNode fieldNode, ClassNode owner) {
        addSetter(fieldNode, owner, ACC_PUBLIC);
    }

    protected void addSetter(FieldNode fieldNode, ClassNode owner, int modifier) {
        ClassNode type = fieldNode.getType();
        String name = fieldNode.getName();
        String setterName = "set" + StringUtils.capitalize(name);
        owner.addMethod(setterName,
                modifier,
                ClassHelper.VOID_TYPE,
                new Parameter[]{new Parameter(nonGeneric(type), "value")},
                null,
                new ExpressionStatement(
                new BinaryExpression(
                new FieldExpression(fieldNode),
                Token.newSymbol(Types.EQUAL, -1, -1),
                new VariableExpression("value"))));
    }
    */
}