package com.umlytics.services;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// GRASP: Pure Fabrication
public class JavaCodeParser implements ICodeParser {
    private final JavaParser javaParser;
    private final List<String> supportedLang;

    private static final Set<String> PRIMITIVES = Set.of(
            "int", "long", "short", "byte", "boolean", "char", "float", "double", "void"
    );

    public JavaCodeParser() {
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setCharacterEncoding(StandardCharsets.UTF_8);
        this.javaParser = new JavaParser(config);
        this.supportedLang = List.of("java");
    }

    @Override
    public UMLModel parse(List<File> files) {
        List<ConceptualClass> parsedClasses = new ArrayList<>();
        Map<String, ConceptualClass> classMap = new HashMap<>();
        Map<ConceptualClass, List<String>> extendsMap = new HashMap<>();
        Map<ConceptualClass, List<String>> implementsMap = new HashMap<>();
        List<String> parseNotes = new ArrayList<>();

        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                throw new UnsupportedFileException("Unsupported file: " + file.getName());
            }
            String simple = file.getName();
            if ("module-info.java".equals(simple) || "package-info.java".equals(simple)) {
                parseNotes.add("Skipped " + simple + " (no diagram types).");
                continue;
            }
            if (!file.isFile() || !file.canRead()) {
                parseNotes.add(simple + ": not readable.");
                continue;
            }
            try {
                CompilationUnit unit = parseCompilationUnit(file);
                String pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

                for (ClassOrInterfaceDeclaration decl : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (decl.isNestedType()) {
                        continue;
                    }
                    ConceptualClass conceptualClass = mapTypeDeclaration(decl);
                    extendsMap.put(conceptualClass, extractExtends(decl));
                    implementsMap.put(conceptualClass, extractImplements(decl));
                    fillFields(decl, conceptualClass);
                    fillMethods(decl, conceptualClass);
                    parsedClasses.add(conceptualClass);
                    registerClassKeys(classMap, pkg, conceptualClass);
                }

                for (RecordDeclaration rd : unit.findAll(RecordDeclaration.class)) {
                    if (rd.isNestedType()) {
                        continue;
                    }
                    ConceptualClass conceptualClass = new ConceptualClass();
                    conceptualClass.setClassId(UUID.randomUUID());
                    conceptualClass.setName(rd.getNameAsString());
                    conceptualClass.setClassType(ClassType.ENTITY);
                    extendsMap.put(conceptualClass, List.of());
                    implementsMap.put(conceptualClass, extractRecordImplements(rd));
                    for (Parameter p : rd.getParameters()) {
                        Attribute attribute = new Attribute();
                        attribute.setAttributeId(UUID.randomUUID());
                        attribute.setAttributeName(p.getNameAsString());
                        attribute.setDataType(p.getTypeAsString());
                        attribute.setStatic(false);
                        attribute.setVisibility(Visibility.PUBLIC);
                        conceptualClass.addAttribute(attribute);
                    }
                    for (MethodDeclaration md : rd.getMethods()) {
                        addMethod(conceptualClass, md);
                    }
                    parsedClasses.add(conceptualClass);
                    registerClassKeys(classMap, pkg, conceptualClass);
                }

                for (EnumDeclaration ed : unit.findAll(EnumDeclaration.class)) {
                    if (ed.isNestedType()) {
                        continue;
                    }
                    ConceptualClass conceptualClass = new ConceptualClass();
                    conceptualClass.setClassId(UUID.randomUUID());
                    conceptualClass.setName(ed.getNameAsString());
                    conceptualClass.setClassType(ClassType.ENUM);
                    implementsMap.put(conceptualClass, extractEnumImplements(ed));
                    extendsMap.put(conceptualClass, List.of());
                    for (EnumConstantDeclaration c : ed.getEntries()) {
                        Attribute attribute = new Attribute();
                        attribute.setAttributeId(UUID.randomUUID());
                        attribute.setAttributeName(c.getNameAsString());
                        attribute.setDataType(ed.getNameAsString());
                        attribute.setStatic(true);
                        attribute.setVisibility(Visibility.PUBLIC);
                        conceptualClass.addAttribute(attribute);
                    }
                    for (MethodDeclaration md : ed.getMethods()) {
                        addMethod(conceptualClass, md);
                    }
                    parsedClasses.add(conceptualClass);
                    registerClassKeys(classMap, pkg, conceptualClass);
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                parseNotes.add(simple + ": " + msg);
            }
        }

        if (parsedClasses.isEmpty()) {
            String detail = parseNotes.isEmpty() ? "" : " — " + String.join("; ", parseNotes);
            throw new ParsingException("No Java types could be parsed from the selected files." + detail, null);
        }

        List<Relationship> relationships = buildRelationships(parsedClasses, classMap, extendsMap, implementsMap);

        UMLModel model = new UMLModel();
        model.setClasses(parsedClasses);
        model.setRelationships(relationships);
        for (String note : parseNotes) {
            model.addParseNote(note);
        }
        return model;
    }

    private CompilationUnit parseCompilationUnit(File file) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(Path.of(file.toURI()));
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            String problems = result.getProblems().stream()
                    .map(Problem::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(problems.isEmpty() ? "Parse failed" : problems);
        }
        return result.getResult().get();
    }

    private static ConceptualClass mapTypeDeclaration(ClassOrInterfaceDeclaration decl) {
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
        return conceptualClass;
    }

    private static List<String> extractExtends(ClassOrInterfaceDeclaration decl) {
        List<String> ext = new ArrayList<>();
        decl.getExtendedTypes().forEach(t -> ext.add(t.getNameAsString()));
        return ext;
    }

    private static List<String> extractImplements(ClassOrInterfaceDeclaration decl) {
        List<String> impl = new ArrayList<>();
        decl.getImplementedTypes().forEach(t -> impl.add(t.getNameAsString()));
        return impl;
    }

    private static List<String> extractRecordImplements(RecordDeclaration rd) {
        List<String> impl = new ArrayList<>();
        rd.getImplementedTypes().forEach(t -> impl.add(t.getNameAsString()));
        return impl;
    }

    private static List<String> extractEnumImplements(EnumDeclaration ed) {
        List<String> impl = new ArrayList<>();
        ed.getImplementedTypes().forEach(t -> impl.add(t.getNameAsString()));
        return impl;
    }

    private static void fillFields(ClassOrInterfaceDeclaration decl, ConceptualClass conceptualClass) {
        for (FieldDeclaration field : decl.getFields()) {
            field.getVariables().forEach(v -> {
                Attribute attribute = new Attribute();
                attribute.setAttributeId(UUID.randomUUID());
                attribute.setAttributeName(v.getNameAsString());
                attribute.setDataType(v.getTypeAsString());
                attribute.setStatic(field.isStatic());
                attribute.setVisibility(extractVisibility(field.isPublic(), field.isProtected(), field.isPrivate()));
                conceptualClass.addAttribute(attribute);
            });
        }
    }

    private static void fillMethods(ClassOrInterfaceDeclaration decl, ConceptualClass conceptualClass) {
        for (MethodDeclaration methodDeclaration : decl.getMethods()) {
            addMethod(conceptualClass, methodDeclaration);
        }
    }

    private static void addMethod(ConceptualClass conceptualClass, MethodDeclaration methodDeclaration) {
        Method method = new Method();
        method.setMethodId(UUID.randomUUID());
        method.setMethodName(methodDeclaration.getNameAsString());
        method.setReturnType(methodDeclaration.getTypeAsString());
        method.setAbstract(methodDeclaration.isAbstract());
        method.setVisibility(extractVisibility(methodDeclaration.isPublic(), methodDeclaration.isProtected(), methodDeclaration.isPrivate()));
        method.setParameters(methodDeclaration.getParameters().toString());
        conceptualClass.addMethod(method);
    }

    private List<Relationship> buildRelationships(
            List<ConceptualClass> parsedClasses,
            Map<String, ConceptualClass> classMap,
            Map<ConceptualClass, List<String>> extendsMap,
            Map<ConceptualClass, List<String>> implementsMap) {
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
        return relationships;
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
        return normalizedSimpleOrFqn.startsWith("java.") || normalizedSimpleOrFqn.startsWith("javax.")
                || normalizedSimpleOrFqn.startsWith("jakarta.");
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

    private static Visibility extractVisibility(boolean isPublic, boolean isProtected, boolean isPrivate) {
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
