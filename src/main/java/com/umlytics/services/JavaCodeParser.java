package com.umlytics.services;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ClassType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.ParsingException;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.interfaces.ICodeParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// GRASP: Pure Fabrication
public class JavaCodeParser implements ICodeParser {
    private final JavaParserLib parserLib;
    private final List<String> supportedLang;

    public JavaCodeParser() {
        this.parserLib = new JavaParserLib();
        this.supportedLang = List.of("java");
    }

    @Override
    public UMLModel parse(List<File> files) {
        List<ConceptualClass> parsedClasses = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                throw new UnsupportedFileException("Unsupported file: " + file.getName());
            }
            try {
                CompilationUnit unit = StaticJavaParser.parse(file);
                unit.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
                    ConceptualClass conceptualClass = new ConceptualClass();
                    conceptualClass.setClassId(UUID.randomUUID());
                    conceptualClass.setName(decl.getNameAsString());
                    if (decl.isInterface()) {
                        conceptualClass.setClassType(ClassType.INTERFACE);
                    } else if (decl.isAbstract()) {
                        conceptualClass.setClassType(ClassType.ABSTRACT);
                    } else {
                        conceptualClass.setClassType(ClassType.ENTITY);
                    }

                    for (FieldDeclaration field : decl.getFields()) {
                        Attribute attribute = new Attribute();
                        attribute.setAttributeId(UUID.randomUUID());
                        attribute.setAttributeName(field.getVariable(0).getNameAsString());
                        attribute.setDataType(field.getVariable(0).getTypeAsString());
                        attribute.setStatic(field.isStatic());
                        attribute.setVisibility(extractVisibility(field.isPublic(), field.isProtected(), field.isPrivate()));
                        conceptualClass.addAttribute(attribute);
                    }

                    for (MethodDeclaration methodDeclaration : decl.getMethods()) {
                        Method method = new Method();
                        method.setMethodId(UUID.randomUUID());
                        method.setMethodName(methodDeclaration.getNameAsString());
                        method.setReturnType(methodDeclaration.getTypeAsString());
                        method.setAbstract(methodDeclaration.isAbstract());
                        method.setVisibility(extractVisibility(methodDeclaration.isPublic(), methodDeclaration.isProtected(), methodDeclaration.isPrivate()));
                        method.setParameters(methodDeclaration.getParameters().toString());
                        conceptualClass.addMethod(method);
                    }
                    parsedClasses.add(conceptualClass);
                });
            } catch (ParseProblemException e) {
                throw new ParsingException("Parsing failed for file: " + file.getName(), e);
            } catch (Exception e) {
                throw new ParsingException("Unexpected parsing failure: " + file.getName(), e);
            }
        }
        UMLModel model = new UMLModel();
        model.setClasses(parsedClasses);
        model.setRelationships(new ArrayList<>());
        return model;
    }

    @Override
    public List<String> getSupportedLanguages() {
        return supportedLang;
    }

    private List<ConceptualClass> extractClasses(AST ast) {
        return new ArrayList<>();
    }

    private List<Relationship> extractRelationships(AST ast) {
        return new ArrayList<>();
    }

    private Visibility extractVisibility(boolean isPublic, boolean isProtected, boolean isPrivate) {
        if (isPublic) {
            return Visibility.PUBLIC;
        }
        if (isProtected) {
            return Visibility.PROTECTED;
        }
        if (isPrivate) {
            return Visibility.PRIVATE;
        }
        return Visibility.PACKAGE;
    }
}
