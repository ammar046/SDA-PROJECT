package com.umlytics.services;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.InheritanceRelationship;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// GRASP: Pure Fabrication
public class JavaCodeParser implements ICodeParser {
    private final JavaParserLib parserLib;
    private final List<String> supportedLang;

    private static final Set<String> PRIMITIVES = Set.of(
            "int", "long", "short", "byte", "boolean", "char", "float", "double", "void"
    );

    public JavaCodeParser() {
        this.parserLib = new JavaParserLib();
        this.supportedLang = List.of("java");
    }

    @Override
    public UMLModel parse(List<File> files) {
        List<ConceptualClass> parsedClasses = new ArrayList<>();
        Map<String, ConceptualClass> classMap = new HashMap<>();
        Map<ConceptualClass, List<String>> extendsMap = new HashMap<>();
        Map<ConceptualClass, List<String>> implementsMap = new HashMap<>();

        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                throw new UnsupportedFileException("Unsupported file: " + file.getName());
            }
            try {
                CompilationUnit unit = StaticJavaParser.parse(file);
                String pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

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

                    List<String> ext = new ArrayList<>();
                    decl.getExtendedTypes().forEach(t -> ext.add(t.getNameAsString()));
                    extendsMap.put(conceptualClass, ext);

                    List<String> impl = new ArrayList<>();
                    decl.getImplementedTypes().forEach(t -> impl.add(t.getNameAsString()));
                    implementsMap.put(conceptualClass, impl);

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
                    registerClassKeys(classMap, pkg, conceptualClass);
                });
            } catch (ParseProblemException e) {
                throw new ParsingException("Parsing failed for file: " + file.getName(), e);
            } catch (Exception e) {
                throw new ParsingException("Unexpected parsing failure: " + file.getName(), e);
            }
        }

        List<Relationship> relationships = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();

        for (ConceptualClass c : parsedClasses) {
            for (Attribute attr : c.getAttributes()) {
                ConceptualClass target = resolveFieldType(attr.getDataType(), classMap);
                if (target != null && target != c) {
                    String key = c.getClassId() + "|assoc|" + target.getClassId() + "|" + attr.getAttributeName();
                    if (edgeKeys.add(key)) {
                        AssociationRelationship r = new AssociationRelationship();
                        r.setRelationshipId(UUID.randomUUID());
                        r.setSourceClass(c);
                        r.setTargetClass(target);
                        r.setSourceMultiplicity("1");
                        String raw = attr.getDataType() == null ? "" : attr.getDataType();
                        r.setTargetMultiplicity(raw.contains("<") || raw.endsWith("[]") ? "*" : "1");
                        relationships.add(r);
                    }
                }
            }
            for (String ext : extendsMap.getOrDefault(c, List.of())) {
                ConceptualClass target = resolveReferenceType(ext, classMap);
                if (target != null && target != c) {
                    String key = c.getClassId() + "|extends|" + target.getClassId();
                    if (edgeKeys.add(key)) {
                        InheritanceRelationship r = new InheritanceRelationship();
                        r.setRelationshipId(UUID.randomUUID());
                        r.setInterface(false);
                        r.setSourceClass(c);
                        r.setTargetClass(target);
                        relationships.add(r);
                    }
                }
            }
            for (String impl : implementsMap.getOrDefault(c, List.of())) {
                ConceptualClass target = resolveReferenceType(impl, classMap);
                if (target != null && target != c) {
                    String key = c.getClassId() + "|impl|" + target.getClassId();
                    if (edgeKeys.add(key)) {
                        InheritanceRelationship r = new InheritanceRelationship();
                        r.setRelationshipId(UUID.randomUUID());
                        r.setInterface(true);
                        r.setSourceClass(c);
                        r.setTargetClass(target);
                        r.setDashed(true);
                        relationships.add(r);
                    }
                }
            }
        }

        UMLModel model = new UMLModel();
        model.setClasses(parsedClasses);
        model.setRelationships(relationships);
        return model;
    }

    private static void registerClassKeys(Map<String, ConceptualClass> classMap, String pkg, ConceptualClass cc) {
        String name = cc.getName();
        classMap.put(name, cc);
        if (pkg != null && !pkg.isBlank()) {
            classMap.put(pkg + "." + name, cc);
        }
    }

    private ConceptualClass resolveFieldType(String rawType, Map<String, ConceptualClass> classMap) {
        String t = normalizeType(rawType);
        if (t.isEmpty() || shouldSkipAssociationTarget(t)) {
            return null;
        }
        return resolveReferenceType(t, classMap);
    }

    private static boolean shouldSkipAssociationTarget(String normalizedSimpleOrFqn) {
        if (PRIMITIVES.contains(normalizedSimpleOrFqn)) {
            return true;
        }
        if ("String".equals(normalizedSimpleOrFqn) || "java.lang.String".equals(normalizedSimpleOrFqn)) {
            return true;
        }
        if (normalizedSimpleOrFqn.startsWith("java.") || normalizedSimpleOrFqn.startsWith("javax.")) {
            return true;
        }
        return false;
    }

    private ConceptualClass resolveReferenceType(String reference, Map<String, ConceptualClass> classMap) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String t = reference.trim();
        ConceptualClass hit = classMap.get(t);
        if (hit != null) {
            return hit;
        }
        int dot = t.lastIndexOf('.');
        if (dot >= 0) {
            String simple = t.substring(dot + 1);
            hit = classMap.get(simple);
            if (hit != null) {
                return hit;
            }
        }
        for (ConceptualClass cc : classMap.values()) {
            if (t.equals(cc.getName()) || t.endsWith("." + cc.getName())) {
                return cc;
            }
        }
        return null;
    }

    /**
     * Strips arrays, generics (including collections), and returns a resolvable class name.
     */
    private static String normalizeType(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        while (t.endsWith("[]")) {
            t = t.substring(0, t.length() - 2).trim();
        }
        int angle = t.indexOf('<');
        if (angle > 0 && t.endsWith(">")) {
            String base = t.substring(0, angle).trim();
            String inner = t.substring(angle + 1, t.length() - 1).trim();
            String baseSimple = base.contains(".") ? base.substring(base.lastIndexOf('.') + 1) : base;
            if (isJavaCollectionFamily(base, baseSimple)) {
                if (inner.contains(",")) {
                    String[] parts = inner.split(",");
                    String last = parts[parts.length - 1].trim();
                    return normalizeType(last);
                }
                return normalizeType(inner);
            }
            if (inner.contains(",")) {
                return normalizeType(inner.split(",")[0].trim());
            }
            return normalizeType(inner);
        }
        return t;
    }

    private static boolean isJavaCollectionFamily(String baseFqn, String baseSimple) {
        return "List".equals(baseSimple) || "Set".equals(baseSimple) || "Collection".equals(baseSimple)
                || "Iterable".equals(baseSimple) || "Optional".equals(baseSimple)
                || baseFqn.startsWith("java.util.");
    }

    @Override
    public List<String> getSupportedLanguages() {
        return supportedLang;
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
