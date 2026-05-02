package com.umlytics.services;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLClass;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.ParsingException;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.interfaces.ICodeParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        List<UMLClass> parsedClasses = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                throw new UnsupportedFileException("Unsupported file: " + file.getName());
            }
            try {
                CompilationUnit unit = StaticJavaParser.parse(file);
                unit.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
                    UMLClass umlClass = new UMLClass();
                    umlClass.setName(decl.getNameAsString());
                    umlClass.setAbstract(decl.isAbstract());
                    umlClass.setInterface(decl.isInterface());

                    for (FieldDeclaration field : decl.getFields()) {
                        Attribute attribute = new Attribute();
                        attribute.setName(field.getVariable(0).getNameAsString());
                        attribute.setType(field.getVariable(0).getTypeAsString());
                        attribute.setStatic(field.isStatic());
                        attribute.setVisibility(extractVisibility(field.isPublic(), field.isProtected(), field.isPrivate()));
                        umlClass.addAttribute(attribute);
                    }

                    for (MethodDeclaration methodDeclaration : decl.getMethods()) {
                        Method method = new Method();
                        method.setName(methodDeclaration.getNameAsString());
                        method.setReturnType(methodDeclaration.getTypeAsString());
                        method.setAbstract(methodDeclaration.isAbstract());
                        method.setVisibility(extractVisibility(methodDeclaration.isPublic(), methodDeclaration.isProtected(), methodDeclaration.isPrivate()));
                        umlClass.addMethod(method);
                    }
                    parsedClasses.add(umlClass);
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

    private List<UMLClass> extractClasses(AST ast) {
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
