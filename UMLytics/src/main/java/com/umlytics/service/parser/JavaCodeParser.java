package com.umlytics.service.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.Method;
import com.umlytics.domain.UMLClass;
import com.umlytics.domain.relationships.InheritanceRelationship;
import com.umlytics.domain.relationships.Relationship;
import com.umlytics.domain.valueobjects.UMLModel;
import com.umlytics.enums.Visibility;
import com.umlytics.interfaces.ICodeParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Java source code to build a UMLModel containing classes and basic relationships.
 * GRASP: Pure Fabrication
 */
public class JavaCodeParser implements ICodeParser {

    @Override
    public List<String> getSupportedLanguages() {
        return List.of("Java");
    }

    @Override
    public UMLModel parse(List<File> files) {
        UMLModel model = new UMLModel();
        Map<String, UMLClass> classMap = new HashMap<>();
        List<Relationship> relationships = new ArrayList<>();

        double currentX = 100.0;
        double currentY = 100.0;

        // Pass 1: Parse and create UMLClass objects, fields, and methods
        for (File file : files) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                for (TypeDeclaration<?> typeDec : cu.findAll(TypeDeclaration.class)) {
                    if (typeDec instanceof ClassOrInterfaceDeclaration coi) {
                        UMLClass umlClass = new UMLClass();
                        umlClass.setName(coi.getNameAsString());
                        umlClass.setInterface(coi.isInterface());
                        umlClass.setAbstract(coi.hasModifier(Modifier.Keyword.ABSTRACT));
                        umlClass.setPositionX(currentX);
                        umlClass.setPositionY(currentY);
                        currentX += 300.0;
                        if (currentX > 1000) { currentX = 100.0; currentY += 250.0; }

                        classMap.put(umlClass.getName(), umlClass);

                        // Parse Attributes
                        for (FieldDeclaration field : coi.getFields()) {
                            for (VariableDeclarator var : field.getVariables()) {
                                Attribute attr = new Attribute();
                                attr.setName(var.getNameAsString());
                                attr.setType(var.getTypeAsString());
                                attr.setVisibility(getVisibility(field));
                                attr.setStatic(field.hasModifier(Modifier.Keyword.STATIC));
                                umlClass.addAttribute(attr);
                            }
                        }

                        // Parse Methods
                        for (MethodDeclaration methodDec : coi.getMethods()) {
                            Method m = new Method();
                            m.setName(methodDec.getNameAsString());
                            m.setReturnType(methodDec.getTypeAsString());
                            m.setVisibility(getVisibility(methodDec));
                            m.setAbstract(methodDec.isAbstract());
                            
                            List<String> params = new ArrayList<>();
                            methodDec.getParameters().forEach(p -> params.add(p.getTypeAsString() + " " + p.getNameAsString()));
                            m.setParameters(params);
                            umlClass.addMethod(m);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to parse " + file.getName() + ": " + e.getMessage());
            }
        }

        // Pass 2: Relationships (extends / implements mapping)
        for (File file : files) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                for (ClassOrInterfaceDeclaration coi : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    UMLClass source = classMap.get(coi.getNameAsString());
                    if (source == null) continue;

                    // Extends
                    coi.getExtendedTypes().forEach(t -> {
                        UMLClass target = classMap.get(t.getNameAsString());
                        if (target != null) {
                            relationships.add(new InheritanceRelationship(0, source, target, false));
                        }
                    });

                    // Implements
                    coi.getImplementedTypes().forEach(t -> {
                        UMLClass target = classMap.get(t.getNameAsString());
                        if (target != null) {
                            relationships.add(new InheritanceRelationship(0, source, target, true));
                        }
                    });
                }
            } catch (Exception ignored) {}
        }

        model.setClasses(new ArrayList<>(classMap.values()));
        model.setRelationships(relationships);
        return model;
    }

    private Visibility getVisibility(com.github.javaparser.ast.nodeTypes.NodeWithModifiers<?> node) {
        if (node.hasModifier(Modifier.Keyword.PUBLIC)) return Visibility.PUBLIC;
        if (node.hasModifier(Modifier.Keyword.PRIVATE)) return Visibility.PRIVATE;
        if (node.hasModifier(Modifier.Keyword.PROTECTED)) return Visibility.PROTECTED;
        return Visibility.PACKAGE;
    }
}
