# UMLytics — Complete Masterclass

> A comprehensive walkthrough of the UMLytics codebase: every file, every class, every key function, mapped to GRASP principles and to the six GoF patterns covered in the course (Factory, Singleton, Adapter, Façade, Observer, Strategy).

---

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
1b. [How to Recognize Each GRASP / GoF Label](#1b-how-to-recognize-each-grasp--gof-label) **← read this first**
2. [Application Entry — `Main.java`](#2-application-entry--mainjava)
3. [Controllers (`com.umlytics.controllers`)](#3-controllers-comumlyticscontrollers)
4. [Database Layer (`com.umlytics.db`)](#4-database-layer-comumlyticsdb)
5. [Domain Layer (`com.umlytics.domain`)](#5-domain-layer-comumlyticsdomain)
6. [Enums (`com.umlytics.enums`)](#6-enums-comumlyticsenums)
7. [Exceptions (`com.umlytics.exceptions`)](#7-exceptions-comumlyticsexceptions)
8. [Interfaces (`com.umlytics.interfaces`)](#8-interfaces-comumlyticsinterfaces)
9. [Repository Implementations (`com.umlytics.repository`)](#9-repository-implementations-comumlyticsrepository)
10. [Service Layer (`com.umlytics.services`)](#10-service-layer-comumlyticsservices)
11. [UI Layer (`com.umlytics.ui`)](#11-ui-layer-comumlyticsui)
11b. [Worked Example — How I Labeled `DiagramController`](#11b-worked-example--how-i-labeled-diagramcontroller)
12. [GRASP — Full Map](#12-grasp--full-map)
13. [GoF — Six Patterns We Covered](#13-gof--six-patterns-we-covered)
14. [End-to-End Use-Case Walkthrough](#14-end-to-end-use-case-walkthrough)

---

## 1. High-Level Architecture

UMLytics is a JavaFX desktop app that builds UML class diagrams three ways: from natural-language descriptions (AI), from `.java` source files (reverse engineering), or from uploaded diagram images (vision AI). It then evaluates the design quality (coupling, cohesion, SOLID) via an LLM and exports to PNG/PDF/SVG.

```
┌──────────────────────────────────────────────────────────┐
│  UI  (com.umlytics.ui, ui.panels, ui.canvas)             │  JavaFX-only
├──────────────────────────────────────────────────────────┤
│  Controllers  (com.umlytics.controllers)                 │  GRASP Controller
├──────────────────────────────────────────────────────────┤
│  Services (LLMAPIEngine, JavaCodeParser, Export, STT)    │  Pure Fabrication
├──────────────────────────────────────────────────────────┤
│  Repositories (interfaces + impl)                        │  Repository pattern
├──────────────────────────────────────────────────────────┤
│  Domain (UMLDiagram, ConceptualClass, Relationship, …)   │  Information Experts
├──────────────────────────────────────────────────────────┤
│  Persistence  (DatabaseManager + SQLite + schema.sql)    │  Singleton
└──────────────────────────────────────────────────────────┘
```

Dependencies point **downward only**. The Domain layer imports nothing JavaFX, nothing JDBC, nothing HTTP. That is what gives UMLytics **Low Coupling** + **High Cohesion** "for free".

---

## 1b. How to Recognize Each GRASP / GoF Label

Use these one-line tests on any class. If the answer is "yes", apply that label.

### GRASP — the nine principles (only the ones used in this project)

| Principle | Single question to ask | If yes, you've found it |
|---|---|---|
| **Information Expert** | "Does this class hold the data needed to perform the responsibility I'm assigning?" | The class is the right owner of the method. |
| **Creator** | "Does class A contain, aggregate, closely use, or have the initialising data for class B?" | A should create B. |
| **Controller** | "Is this class the first non-UI object handling a system event (use-case)?" | It's a Controller. |
| **Low Coupling** | "If I changed this class, would few others have to change?" | Coupling is low — usually because we coded against an interface. |
| **High Cohesion** | "Do all members of this class serve **one** clear responsibility?" | Cohesion is high. |
| **Polymorphism** | "Am I switching behaviour based on a type/enum check?" If yes, push it into subclasses. | Polymorphism replaces the switch. |
| **Pure Fabrication** | "Does this class represent a real-world domain concept?" If **no** but it exists to keep the design clean (DB, HTTP, parser, etc.), it's fabricated. | Pure Fabrication. |
| **Indirection** | "Is there a middle object that exists to break a direct coupling between two others?" | That middle object is the Indirection. |
| **Protected Variations** | "Is something behind a stable interface so I can swap implementations later?" | The interface is the protected variation point. |

### GoF — the six patterns covered in the course

| Pattern | Single question to ask | Telltale sign in code |
|---|---|---|
| **Factory** (Method) | "Does this method return a fully-built object of an *abstract* type, hiding which concrete class it is?" | A method whose return type is an interface/abstract class but which `new`s up one of several concretes inside. |
| **Singleton** | "Is there exactly one of this object, with a private constructor and a static `getInstance()` accessor?" | `private static instance` + `private constructor` + `getInstance()`. |
| **Adapter** | "Does this class translate between an *external* API and our *internal* interface?" | Class implements our interface and *contains* / *calls* the foreign API. |
| **Façade** | "Does this class expose a single, simple API in front of a more complicated subsystem?" | One class with a small public surface hiding many collaborators. |
| **Observer** | "Does object A notify object B (or many Bs) whenever A's state changes, without A knowing who B is?" | Listener/handler fields, `setOn*` callbacks, `Consumer`/`Runnable` callbacks. |
| **Strategy** | "Can I swap the algorithm at runtime by replacing one object with another that implements the same interface?" | A field typed as an interface; multiple `Impl` classes; switch happens by injection. |

> **Tip for the report/viva:** for every class you label, finish the sentence "*This is a Controller **because** \_\_\_*" with one **observable** code fact (an interface it depends on, a method it routes through, a field it owns, a type its return is). If you can't, the label is probably wrong.

---

## 2. Application Entry — `Main.java`

```java
public class Main {
    public static void main(String[] args) {
        Application.launch(MainWindow.class, args);
    }
}
```

**Why so empty?** Java 17+ with the JavaFX module path requires that the class containing `main` **not** be the same class as the `Application` subclass when launched via fat-jars. So `Main` is a thin **bootstrapper** for `MainWindow`. It keeps `MainWindow` testable and module-friendly.

---

## 3. Controllers (`com.umlytics.controllers`)

The **only** layer the UI is allowed to call. Contains no JavaFX and no SQL — purely orchestration.

### 3.1 `DiagramController.java`

Coordinates everything that touches a diagram.

**Fields**
- `IDiagramRepository diagramRepo` — persistence
- `IAIEngine aiEngine` — natural-language + vision
- `ICodeParser codeParser` — `.java` → UML
- `IExportService exportSvc` — PNG/PDF/SVG export

**Key methods**

| Method | What it does |
|---|---|
| `generateFromText(UUID, String)` | Validates description ≥ 10 chars → `aiEngine.generateFromText` → tags `SourceType.NATURAL_LANGUAGE` → `repo.save`. |
| `generateFromCode(UUID, List<File>)` | Filters non-`.java` files (throws `UnsupportedFileException`), delegates to `codeParser.parse`, sets `SourceType.SOURCE_CODE`. |
| `modifyClassDefinition(UUID, DiagramEdit)` | Loads diagram → `edit.apply(diagram)` → `pruneInvalidRelationships()` → `repo.update`. |
| `analyzeUploadedImage(UUID, byte[])` | Sanity-checks magic bytes (PNG `89 50 4E 47`, JPG `FF D8 FF`), 5 MB cap, then vision API. |
| `exportDiagram(UUID, ExportFormat, String)` | Pure delegation to `IExportService` — controller never touches Batik/PDFBox directly. |
| `evaluateDesign(UMLDiagram)` | Refuses < 2 classes (`DiagramTooSimpleException`) — use-case business rule. |
| `generateStructureSuggestions(...)` | LLM call for Java skeletons; wraps result in `ClassSuggestion`. |
| `pruneInvalidRelationships(...)` | Private: drops edges whose endpoints were deleted. Information Expert — the controller owns diagram invariants. |

**Labels — and why:**
- **GRASP Controller** — every UI gesture related to a diagram (generate, modify, evaluate, export, save, delete, rename) goes through this single class first. It's the first non-UI object touching the use-case event. The UI never calls a repository or an `IAIEngine` directly.
- **GRASP Creator** — the constructor injects four collaborators but the class also *creates* domain objects mid-flow: `DesignEvaluationReport` (in `evaluateDesign`, sets `reportId`, `diagramId`, `projectId`, `evaluationDate`), `ClassSuggestion` (in `generateStructureSuggestions`, sets `suggestionId`, `diagramId`), and assigns fresh `UUID`s for diagrams in `saveDiagram`. By GRASP's rule "B contains/aggregates/initialises A → B creates A" — the controller has all the initialising data for those reports/suggestions.
- **GRASP Information Expert** — only this class sees *both* the diagram (from the repo) and the AI/parser results, so it's the only place that knows enough to enforce diagram-level invariants. `pruneInvalidRelationships(UMLDiagram)` is the textbook proof: it walks the classes-set, then removes any relationship whose endpoint UUID is no longer present. Nobody else has both pieces of information.
- **GoF Façade** — `DiagramController` hides the AI engine + code parser + repository + export-service subsystem behind one typed API. A caller writes `diagramCtrl.generateFromText(projectId, desc)` and never sees `OkHttp`, `JavaParser`, `PreparedStatement`, or `PDFBox`.
- **GoF Strategy (client of)** — the fields `aiEngine`, `codeParser`, `exportSvc` are **typed by the interface**, not the implementation. The controller is the *client* in the Strategy pattern: it never knows whether the AI is OpenAI or Gemini, whether the parser is Java or Python, whether the export is PNG or PDF. Swapping any of these is one constructor argument away.

### 3.2 `AIController.java`

Handles AI co-pilot interactions.

**Fields:** `IAIEngine`, `IChatRepository`, `IEvaluationRepository`, `IDiagramRepository`.

**Key methods**
- `evaluateDesign(UUID)` — fetches diagram, builds prompt payload via `AiDiagramPayload.evaluationPayload`, calls LLM, persists report.
- `submitDesignQuestion(text, projectId, diagramId, liveDiagram)` — saves the **user** message → builds `ProjectContext` (chat history + live canvas) → calls `aiEngine.consultDesign` → saves the **AI** message. Dual save guarantees a perfect transcript even if a later request fails.
- `generateStructureSuggestions(...)` — asks LLM for Java skeletons, runs the result through `ClassSuggestion.combineSkeletonResponse(...)` which flattens the LLM's JSON shape.

**Labels — and why:**
- **GRASP Controller** — same test as 3.1: it's the first non-UI object that handles AI-related use-case events (Evaluate, Ask a Design Question, Generate Structure). The chat panel hands the user's text straight to `submitDesignQuestion(...)`.
- **GRASP Pure Fabrication** — *nothing* in the conceptual model is called "AI Controller". The class exists only to keep the UI free of HTTP and to keep the domain free of chat persistence concerns. Larman's definition exactly: a class invented to assign responsibilities cleanly, not derived from the problem domain.
- **GoF Façade** — it hides three subsystems (the LLM, the chat repository, the evaluation repository) behind one typed API: `submitDesignQuestion`, `evaluateDesign`, `generateStructureSuggestions`. The chat panel never knows that a single "ask a question" call writes two rows to SQLite.

### 3.3 `ProjectController.java`

CRUD for `Project` aggregates.

- `createProject(name, desc)` — enforces unique name (`equalsIgnoreCase`); throws `ValidationException` on duplicates.
- `openProject(UUID)` — **eager hydration**: loads project + diagrams + chat history + evaluation history in one call. This is the DDD Aggregate Root pattern baked into the controller.
- `updateProjectMetadata(UUID, name, desc)` — validates uniqueness ignoring the current row.
- `deleteProject(UUID)`, `getAllProjects()`, `findProject(UUID)` — light wrappers.

**Labels — and why:**
- **GRASP Controller** — every project use-case (new/open/save/rename/delete) goes through this one object before reaching any repository. The toolbar's `New Project` button calls `projectController.createProject(...)` — that's the textbook Controller test.
- **GRASP Creator** — `createProject(...)` calls `Project.createProject(name, desc)` (the domain Factory Method) and then enriches it with the duplicate-name guard. The controller "has the initialising data" — it knows the candidate name and the existing project list — so it's the right object to create the new `Project`.
- **GRASP Information Expert (for cross-aggregate validation)** — only the controller sees both *the candidate project name* and *every existing project*; only it can enforce the uniqueness invariant.
- **GoF Façade** — hides `IProjectRepository`, `IDiagramRepository`, `IChatRepository`, and `IEvaluationRepository` behind a single "open this project" call (`openProject` returns a fully hydrated aggregate, four repository calls deep).

---

## 4. Database Layer (`com.umlytics.db`)

### 4.1 `DatabaseManager.java` — **Singleton**

- `private static DatabaseManager instance` + `getInstance()` `synchronized` → classic Java Singleton (lazy, thread-safe).
- `initialize()` opens a JDBC connection, splits `schema.sql` on `;`, executes each DDL statement, then runs `applySchemaMigrations(...)` — a non-destructive back-compat layer that copies data from legacy plural tables (`projects`, `uml_diagrams`, `uml_classes`, ...) into the new singular schema. Each migration block is in its own `try/ignored` so a missing legacy table is harmless.
- `getConnection()` first borrows from `ConnectionPool`, falls back to a fresh `DriverManager` connection.
- `executeQuery(sql)` / `executeUpdate(sql)` — convenience methods used by tests; transactional with `commit/rollback`.
- `loadConfig()` reads `db.url`, `db.username`, `db.password` from `config.properties` with sane SQLite defaults.

**Labels — and why:**
- **GoF Singleton** — the implementation has every textbook marker: `private DatabaseManager() { ... }` (no public constructor), `private static DatabaseManager instance;`, and `public static synchronized DatabaseManager getInstance()` that lazy-creates exactly one instance. Why we *want* a singleton here: the database file is a single resource — multiple manager instances would race on schema initialisation and could corrupt the connection pool.
- **GRASP Pure Fabrication** — "Database Manager" is not in our domain glossary (a software designer doesn't reason about JDBC connections). The class was invented purely so the rest of the system can stay clean of JDBC URL strings, schema migrations, and config loading. Larman's exact criterion: a class that doesn't represent a domain concept but exists to assign responsibilities cleanly.
- **GRASP Indirection** — every layer above it asks `DatabaseManager.getInstance().getConnection()`; nobody calls `DriverManager.getConnection(...)` themselves. That single hop is the indirection.

### 4.2 `ConnectionPool.java`

A 19-line wrapper around an `ArrayDeque<Connection>` with synchronized `borrow()` / `release()`. An **Object Pool** (GoF-adjacent) — caches clean connections so the JavaFX worker threads don't open a fresh JDBC socket per event.

**Labels — and why:**
- **GRASP Pure Fabrication** — like `DatabaseManager`, an Object Pool is not part of the domain — it's a performance fabrication.
- **GRASP Information Expert** — only this class knows the deque of idle connections, so it's the right owner of `borrow()` / `release()`.
- **Not Singleton** — it's deliberately instantiable so tests can use their own pool; `DatabaseManager` owns the *single* runtime instance.

---

## 5. Domain Layer (`com.umlytics.domain`)

Pure Java — no JavaFX, no JDBC, no HTTP. Every class here is an **Information Expert**: it knows about its own state and exposes the minimum needed for callers to make decisions.

### 5.1 Aggregates and entities

#### `UMLDiagram.java` — **the Aggregate Root**
- Owns `List<ConceptualClass> classes` + `List<Relationship> relationships`.
- Tracks visual defaults (`defaultClassHeaderColor`, `defaultEdgeColor`, ...) so new classes inherit visual identity.
- `addConceptualClass`, `removeConceptualClass`, `addRelationship`, `removeRelationship` — each mutator updates `lastModifiedDate`. The diagram knows when it became dirty.
- `serialize()` — hand-rolled JSON for embedding the diagram in chat prompts (layout-only — colors, positions, sizes).
- `validate()` — invariant check.

**Labels — and why:**
- **GRASP Information Expert** — `UMLDiagram` *contains* the lists of classes and relationships and the `lastModifiedDate` field. Therefore by GRASP, it is the right object to expose `addConceptualClass`, `removeConceptualClass`, `addRelationship`, `removeRelationship`, `serialize`, and `validate`. Note that the mutators all touch `lastModifiedDate` — that's only possible because the diagram owns that field.
- **GRASP Creator** — when you call `addConceptualClass(new ConceptualClass())` from inside `DiagramEdit.apply`, the diagram *aggregates* the class — by the Creator rule "B aggregates A → B is a natural creator of A", the diagram is the place that owns class lifecycle.
- **GRASP High Cohesion** — every field and method is about *one* UML diagram. Nothing about persistence, nothing about rendering. Move any one method out and the class still makes sense.

#### `UMLModel.java` — a transient DTO
Used to ferry classes + relationships between the LLM/parser and the diagram. It has:
- `toUMLDiagram()` — a **Factory Method** that builds a new `UMLDiagram`, generates its UUID, and copies classes & relationships in.
- `rawJson` — original LLM payload for re-prompting and debugging.
- `parseNotes` — warning breadcrumbs the JavaCodeParser leaves behind.

**Labels — and why:**
- **GoF Factory Method** — `toUMLDiagram()` returns a fully-built `UMLDiagram` *and assigns its UUID*. The caller (`DiagramController.generateFromText`, `generateFromCode`, `analyzeUploadedImage`) never `new`s up a diagram itself. That is the exact intent of Factory Method: hide the construction recipe behind a typed method on a sibling object.
- **GRASP Creator** — `UMLModel` contains the candidate classes and relationships — it has the initialising data — so by Creator, it's the right object to build the resulting `UMLDiagram`.

#### `ConceptualClass.java` — one UML class
Fields: `classId`, `diagramId`, `name`, `classType` (enum), `visibility`, position (x,y), `headerColor`, `borderColor`, `memberFontSize`, `classWidth`, `classHeight`, attributes, methods.

Helpers like `isAbstract()` / `isInterface()` / `isEnum()` and the symmetric mutators offer a friendly API over the `ClassType` enum.

#### `UMLClass.java`
Empty subclass of `ConceptualClass`. The SDA class diagram named the type `UMLClass`; the implementation uses `ConceptualClass`. This subclass is a **type alias** so both names compile and the code matches the design artifact word-for-word.

#### `Attribute.java` / `Method.java`
Standard UML members. `Method.getSignature()` returns `name(params): returnType`. Both have `Visibility` plus `isStatic` / `isAbstract` flags.

#### `Project.java`
Workspace aggregate: name + description + dates + nested `List<UMLDiagram>` + `List<ChatMessage>` + `List<DesignEvaluationReport>`. `createProject(name, desc)` is a **Factory Method** that sets timestamps and the UUID.

**Labels — and why:**
- **GoF Factory Method** — `Project.createProject(name, desc)` returns a `Project` with a fresh `projectId`, `createdDate`, and `lastModifiedDate`. Callers (`ProjectController.createProject`) never call `new Project()` themselves. The recipe is hidden behind the method.
- **GRASP Information Expert** — `Project` owns the nested lists, so `addDiagram`, `removeDiagram`, `getDiagrams`, etc., live here. Same logic as `UMLDiagram`.

### 5.2 Polymorphic relationships

#### `Relationship.java` — abstract base
Shared state (UUID, source/target classes, multiplicities, label, `bendX`, color, dashed) and one abstract method: `getType()`.

#### Three concrete subclasses — **GRASP Polymorphism in action**

- **`AssociationRelationship`** — has `isComposition`, `isAggregation`, `Navigability`. Its `getType()` returns `COMPOSITION` / `AGGREGATION` / `ASSOCIATION` based on flags.
- **`InheritanceRelationship`** — has `isInterface`. Returns `REALIZATION` for `implements`, `INHERITANCE` for `extends`.
- **`DependencyRelationship`** — has `dependencyType`. Always returns `DEPENDENCY`.

This means `DiagramExportService.drawRelationship`, `RelationshipEdge.setRelationshipType`, and `LLMAPIEngine.parseUmlModelFromJson` all dispatch via `switch(rel.getType())` — never `instanceof`. Subclasses decide their own behaviour.

**Why this is GRASP Polymorphism (the explicit test):**
- The decision "what kind of relationship am I?" is answered *by the subclass itself* through `getType()`. There is no central `if (rel instanceof AssociationRelationship) ...` chain anywhere — that's the smell the principle eliminates.
- If we add a new edge type tomorrow (e.g. `UsageRelationship`), we add one subclass and one enum value; we do **not** touch `DiagramExportService` or the parser logic in any other way.
- That's exactly Larman's wording for the Polymorphism principle: "when related alternatives or behaviour vary by type, assign the responsibility for the behaviour to the types for which the behaviour varies." We did.

> The `Relationship` hierarchy is *not* GoF Strategy: it isn't an algorithm injected into a context — it's a domain class hierarchy. Strategy in this project lives on the *service* interfaces (see §10).

### 5.3 Edit & AI auxiliary

#### `DiagramEdit.java`
Encapsulates a canvas mutation as an object (one frame of the undo stack).

**Labels — and why:**
- **GoF Command (course-adjacent)** — `DiagramEdit` packages a request as an object so the caller can store it and replay it later. `DiagramEditorPanel.undo_internal()` pops one and calls `edit.apply(...)` on the inverse. Even though Command isn't one of the six patterns we covered, this is the canonical use of the pattern in the project.
- **GRASP Information Expert** — the edit object owns `editType`, `targetClassId`, and the `payload` map — i.e. *everything* needed to replay itself. Therefore `apply(UMLDiagram)` is on `DiagramEdit`, not on the editor panel.

```java
public void apply(UMLDiagram diagram) {
    switch (editType) {
        case ADD_CLASS    -> diagram.addConceptualClass(new ConceptualClass());
        case REMOVE_CLASS -> diagram.removeConceptualClass(targetClassId);
        case RENAME_CLASS -> { /* iterate, mutate matching id */ }
        default           -> { /* extra ops added in editor */ }
    }
}
```
- Has `editType` (`EditType` enum), `targetClassId` (UUID), `Map<String,Object> payload`.
- Pushed onto `undoStack` / `redoStack` in `DiagramEditorPanel`.

#### `ChatMessage.java`
Row-shape: `messageId`, `content`, `SenderType sender`, `timestamp`, `projectId`, optional `classId`.

#### `DesignEvaluationReport.java`
- Floats 1–10: `couplingScore`, `cohesionScore`, `solidScore`.
- `getOverallScore()` = average.
- `getSummary()` falls back to a templated string when the LLM omits the summary key.
- `suggestions` list (returned unmodifiable).

#### `ClassSuggestion.java`
Wraps an AI-generated Java skeleton.
- Static `combineSkeletonResponse(raw, allowedNames)` is the cleanup pipeline:
  1. Detects pure-text vs JSON.
  2. Normalizes provider-specific JSON shapes (via `SkeletonResponseNormalizer`).
  3. Filters keys to classes actually present in the diagram.
  4. Concatenates `// === ClassName ===` headers + Java source into one readable string.
  5. Appends `// === (missing) Foo ===` for classes the model forgot.
- `getSkeletons()` re-parses the stored `skeletonCode` back into `Map<String, String>` when needed.

#### `ProjectContext.java`
The "package" of state shipped with each chat question: current `Project`, `currentDiagram`, `chatHistory`. `buildContextPrompt()` returns a short header used in offline / debug paths.

#### `SourceFile.java`, `DiagramImage.java`, `SoftwareDesigner.java`, `VoiceInput.java`
POJO scaffolds for use cases that the class diagram requires for traceability but aren't yet fully persisted. They exist so the implementation matches the SDA design artifact.

---

## 6. Enums (`com.umlytics.enums`)

A clean, type-safe vocabulary. Each enum replaces a `String` constant or a `boolean` somewhere.

| Enum | Values | Used by |
|---|---|---|
| `ClassType` | `ENTITY, ABSTRACT, INTERFACE, ENUM, PACKAGE` | `ConceptualClass.classType` |
| `EditType` | `ADD_CLASS, REMOVE_CLASS, RENAME_CLASS, ADD/REMOVE_ATTRIBUTE, ADD/REMOVE_METHOD, ADD/REMOVE_RELATIONSHIP, MOVE_CLASS` | `DiagramEdit` switch — defines the undo vocabulary |
| `ExportFormat` | `PNG, PDF, SVG` | `IExportService` |
| `FileLanguage` | `JAVA, PYTHON, CPP, CSHARP, OTHER` | `SourceFile.language` |
| `ImageFormat` | `PNG, JPG, JPEG` | `DiagramImage.imageFormat` |
| `Navigability` | `UNIDIRECTIONAL, BIDIRECTIONAL, NONE` | `AssociationRelationship` |
| `RelationshipType` | `INHERITANCE, COMPOSITION, AGGREGATION, DEPENDENCY, REALIZATION, ASSOCIATION` | dispatch tag returned by `Relationship.getType()` |
| `RenderState` | `PENDING, RENDERING, RENDERED, ERROR` | `UMLDiagram.renderState` — a small State machine |
| `SenderType` | `USER, AI, SYSTEM` | `ChatMessage.sender` — UI uses it for bubble alignment |
| `SourceType` | `NATURAL_LANGUAGE, SOURCE_CODE, MANUAL, UPLOADED_IMAGE` | `UMLDiagram.sourceType` |
| `Visibility` | `PUBLIC, PRIVATE, PROTECTED, PACKAGE` | `Attribute`, `Method`, `ConceptualClass` |

---

## 7. Exceptions (`com.umlytics.exceptions`)

All extend `RuntimeException` (unchecked) so they bubble out of services and into a single catch in the UI thread.

**Business / validation (recoverable, shown as toasts):**
- `ValidationException` — generic field validation
- `EmptyDiagramException` — operation needs ≥ 1 class
- `DiagramTooSimpleException` — evaluation needs ≥ 2 classes
- `UnsupportedFileException` — non-`.java` source or non-PNG/JPG image
- `UnsupportedFormatException` — export target not in `IExportService.getSupportedFormats()`
- `DiagramValidationWarning` — soft warning

**Infrastructure (logged):**
- `DatabaseException` (with cause)
- `AIEngineException` (with cause)
- `ParseResponseException` (with cause) — bad LLM JSON
- `ParsingException` (with cause) — bad Java source
- `HardwareException` — no mic detected
- `SpeechServiceException` (with cause)

Uniform `(message)` or `(message, cause)` constructors mean any controller can wrap a low-level exception once and the UI never has to remember a checked-exception list.

---

## 8. Interfaces (`com.umlytics.interfaces`)

The contract seams — controllers depend on these abstractions, not the implementations. This is where Dependency Inversion lives.

- **`IAIEngine`** — `generateFromText`, `evaluateDesign`, `consultDesign`, `generateStructure`, `analyzeImage`. Implemented by `LLMAPIEngine`. **Strategy** for AI providers.
- **`ICodeParser`** — `parse(List<File>)` + `getSupportedLanguages()`. Implemented by `JavaCodeParser`. Strategy seam for future Python/C# parsers.
- **`IDiagramRepository` / `IProjectRepository` / `IChatRepository` / `IEvaluationRepository`** — the **Repository** pattern. Each has CRUD-like methods returning domain objects, never raw `ResultSet`s.
- **`IExportService`** — `export(diagram, fmt, path)` + `getSupportedFormats()`. Strategy.
- **`ISpeechToTextService`** — `startRecording`, `stopRecording`, `isAvailable`. Implemented by `SpeechToTextServiceImpl`.

**Why every interface here is GoF Strategy + GRASP Protected Variations:**
- Each interface declares an *abstract operation*. The concrete classes (e.g. `LLMAPIEngine`, `JavaCodeParser`, `DiagramExportService`) provide *interchangeable* implementations. A controller field of the interface type is the *context*; the concrete class is the *strategy*. That matches the Strategy template exactly.
- The interface is the **stable surface** — the controllers don't change when we add a Gemini provider, a Python parser, or an SVG export. That is the textbook test for **Protected Variations** ("identify points of predicted variation and protect them with stable interfaces").
- Bonus: this is also **GRASP Low Coupling** — the number of incoming dependencies on each concrete class is **zero** because everyone depends on the interface.

---

## 9. Repository Implementations (`com.umlytics.repository`)

All implementations are **stateless**, use **`PreparedStatement`** everywhere (parameterized — SQL-injection safe), and wrap exceptions in `DatabaseException`. All use `conn.setAutoCommit(false)` + explicit `commit`/`rollback` for safety.

### 9.1 `ProjectRepositoryImpl.java`
Straightforward `save / update / delete / findById / findAll`. `mapProject(ResultSet)` is a **Row Mapper**.

### 9.2 `DiagramRepositoryImpl.java` — the heaviest
A diagram is a tree: classes → attributes + methods, plus relationships. The repository:
1. `save(UMLDiagram)` wraps everything in one transaction (`insertDiagramRow` + `insertDiagramContent`).
2. `update(UMLDiagram)` does **delete-and-reinsert** for children. Avoids per-row change tracking; simple and atomic.
3. `findById(UUID)` runs several `SELECT`s and uses `mapRelationship` to **promote** the `relationship_type` text column back into the right concrete subclass (`AssociationRelationship` / `InheritanceRelationship` / `DependencyRelationship`) — the inverse of polymorphism, a hand-rolled discriminator load.
4. `findByProject(UUID)` lists IDs, then loops over `findById` to hydrate each.

### 9.3 `ChatRepositoryImpl.java`
Trivial CRUD. Defaults nulls before insert.

### 9.4 `DesignEvaluationRepositoryImpl.java`
Same shape. `findByDiagram(UUID)` returns the most recent report (`ORDER BY evaluation_date DESC LIMIT 1`).

These four classes are textbook **Repository** + **Pure Fabrication**: nothing in the conceptual model is "a repository", but we fabricate one to keep the rest of the system clean (Larman's GRASP).

**Labels — and why (applies to all four repository impls):**
- **GRASP Pure Fabrication** — apply the test from §1b: "Does this class represent a real-world domain concept?" *No* — a designer doesn't reason about a "DiagramRepositoryImpl". The class exists purely to keep JDBC out of the domain layer.
- **GRASP Information Expert** — the impl is the only place that knows the JDBC schema, so it owns the SQL and the row mapping. The domain doesn't.
- **GRASP Low Coupling** — controllers depend on the interface, not on these impls. We can drop `DiagramRepositoryImpl` and replace it with a `FileSystemDiagramRepositoryImpl` without touching any controller.
- **GoF Adapter (mild)** — these classes adapt the JDBC API (`PreparedStatement` / `ResultSet`) to our domain API (`UMLDiagram`, `Relationship`, etc.). The mapping from `relationship_type` string column to the concrete `Relationship` subclass in `DiagramRepositoryImpl.mapRelationship(...)` is the clearest example of this translation work.

---

## 10. Service Layer (`com.umlytics.services`)

External integrations and CPU-bound transformations. Implements the interfaces above.

### 10.1 `LLMAPIEngine.java` (~1300 lines) — the LLM gateway
Implements `IAIEngine`. Speaks **OpenAI-compatible** endpoints (OpenAI, Cerebras, …) by default and **Google Gemini** when `ai.provider=gemini` or only a Gemini key is supplied.

Highlights:
- **Adapter:** outwardly "give me a `UMLModel` from text"; inwardly constructs JSON via `OkHttp` and translates responses back.
- **Adapter (second layer):** `convertOpenAiChatRequestToGemini(...)` re-encodes the OpenAI chat shape into Gemini's `generationConfig + systemInstruction + contents` shape.
- Three carefully separated **system prompts**:
  - `SYSTEM_UML_JSON` — strict JSON schema for diagram generation
  - `SYSTEM_JAVA_SKELETON_JSON` — strict skeleton schema for code generation
  - `SYSTEM_DESIGN_CONSULT` — natural-English design tutor for chat
- Each `call…Chat` method picks the right system prompt. Mixing them is the classic failure mode for AI co-pilots; the code explicitly comments to warn against re-use.
- `evaluateDesign(...)` uses `temperature=0` + optional `seed` for reproducibility, and `response_format: {"type":"json_object"}`. If that fails, retries without the strict format flag — graceful degradation.
- `parseEvaluationJson(...)` is heavily defensive: pulls scores from multiple key variants, clamps to 1–10, regex fallback when JSON parsing fails, and validates that the summary actually references at least one class name from the diagram (`validateEvaluationAgainstDiagram`) — catches AI hallucination.
- `compressDiagramImage(byte[])` downscales large uploads to ≤ 1536 px JPEG so the payload fits provider limits.

**Labels — and why:**
- **GoF Adapter** — apply the test: "Does this class translate between an *external* API and our *internal* interface?" Yes — `IAIEngine` is our internal contract; the OpenAI HTTP API (and Gemini HTTP API) are the external ones. `LLMAPIEngine` calls `okhttp3.OkHttpClient` and `gson.Gson` to convert one to the other. The double-adapter angle is `convertOpenAiChatRequestToGemini(...)`, which adapts the OpenAI request *shape* into the Gemini shape so the rest of the class can stay OpenAI-flavoured.
- **GoF Strategy (the implementation)** — `IAIEngine` is the strategy interface (see §8); `LLMAPIEngine` is one strategy. Tomorrow we add `LocalOllamaAIEngine` as a second strategy and the constructor wiring in `MainWindow.start` is the only line that changes.
- **GRASP Pure Fabrication** — "LLM API Engine" is not a domain concept; it's invented to keep HTTP out of the controllers.
- **GRASP Protected Variations** — the entire pile of "if Gemini do X, if OpenAI do Y, if the response_format flag fails retry without it" is hidden behind the `IAIEngine` interface. None of that complexity leaks out.

### 10.2 `JavaCodeParser.java` — `.java` → UML
Wraps **JavaParser** (`com.github.javaparser`).
- `parse(List<File>)` walks each `CompilationUnit` and maps:
  - `ClassOrInterfaceDeclaration` (top-level only) → `ConceptualClass` (`ABSTRACT` / `INTERFACE` / `ENTITY`)
  - `RecordDeclaration` → `ConceptualClass` whose components become public attributes
  - `EnumDeclaration` → `ConceptualClass` (`ENUM`) with one attribute per constant
- `buildRelationships(...)` builds three flavours of edges:
  1. **Association** from each field whose type resolves to another parsed class (multiplicity `*` if the field is `List<X>` / `X[]`, else `1`).
  2. **Inheritance** from `extends` clauses.
  3. **Realization** (dashed) from `implements` clauses.
- `normalizeType(String)` strips `[]`, unwraps `Optional<X>`, peels off `java.util.*` wrappers.
- `shouldSkipAssociationTarget(...)` excludes `java.*` / `javax.*` / `jakarta.*` / `String` / primitives so you don't get an edge to `String` from every field.

This is an **Adapter** over JavaParser plus an in-spirit **Visitor** (uses `unit.findAll(...)`).

**Labels — and why:**
- **GoF Adapter** — `ICodeParser` (our interface) hides `com.github.javaparser.*` (third-party API). `JavaCodeParser` is the only class that imports both. The translation work is in the mapping methods that convert `ClassOrInterfaceDeclaration` → `ConceptualClass`, `FieldDeclaration` → `AssociationRelationship`, etc.
- **GoF Strategy (the implementation)** — `ICodeParser` is the strategy interface; `JavaCodeParser` is the first concrete strategy. The seam is ready for a `PythonCodeParser` or `CSharpCodeParser`.
- **GRASP Pure Fabrication** — "Java Code Parser" is not in the domain model.
- **GRASP Creator** — the parser is the only object with enough information (the `CompilationUnit`) to instantiate `ConceptualClass`, `Attribute`, `Method`, and the relationship edges. By "B has the initialising data for A → B creates A", it's the natural creator of those domain objects.

### 10.3 `DiagramExportService.java` — PNG / PDF / SVG
Implements `IExportService`. Single `export(diagram, fmt, path)` dispatches by format:
- **PNG** — renders a `BufferedImage` via AWT `Graphics2D`, writes through `ImageIO`.
- **PDF** — Apache **PDFBox**: renders to bitmap, embeds via `LosslessFactory.createFromImage`, draws on a `PDPage` sized to the bitmap.
- **SVG** — Apache **Batik** `SVGGraphics2D`, streams the DOM.

All three share the same `paintDiagram(Graphics2D, …)` core so visuals are identical. `drawArrowMarker` picks the right arrowhead (filled diamond/composition, hollow diamond/aggregation, hollow triangle/inheritance/realization, open arrow/association/dependency).

**Labels — and why:**
- **GoF Strategy (multi-format dispatch)** — `IExportService.export(diagram, fmt, path)` is the *strategy interface*, and the three branches in `DiagramExportService.export(...)` are essentially three internal strategies switched by `ExportFormat`. (Strictly, the classic Strategy form would split them into `PngExportStrategy`, `PdfExportStrategy`, `SvgExportStrategy` — we kept it as one class for cohesion and shared `paintDiagram` code. The seam is still on the interface, so it satisfies Protected Variations.)
- **GoF Adapter** — `DiagramExportService` adapts the AWT/PDFBox/Batik APIs (each external, each with its own `Graphics2D` flavour) to our domain API (`UMLDiagram` + `ExportFormat`).
- **GRASP Pure Fabrication** — "Diagram Export Service" isn't a domain concept; it's invented to keep file I/O and rendering libs out of the controllers.
- **GRASP Polymorphism (consumer of)** — `drawRelationship` dispatches on `Relationship.getType()`, which itself is polymorphic per subclass — see §5.2.

### 10.4 `AiDiagramPayload.java`
Static utility that turns a `UMLModel` into the **richer JSON** the LLM needs for evaluation / code-gen. `UMLDiagram.serialize()` is layout-only and excludes attributes/methods; `AiDiagramPayload.summarizeModelAsJson(...)` is the prompt-grade JSON with full members and relationships. The class Javadoc explicitly contrasts the two.

### 10.5 `SkeletonResponseNormalizer.java`
Static utility that cleans LLM skeleton JSON. Three pipeline methods: `toCleanSkeletonsJson` → `filterSkeletonKeys` → `skeletonsToMap`. `sanitizeJavaString` rejects pseudo-Java like `{"package":"…"}`. An **Adapter**/Pipes-and-Filters utility.

### 10.6 Stubs & STT
- `AST.java`, `JavaParserLib.java`, `RenderEngine.java` — empty placeholders referenced by the SDA class diagram.
- `AudioCapture.java` — minimal `recording` flag for the STT pipeline.
- `STTAPIClient.java` — stub `transcribe(byte[])` returning `""`.
- `SpeechToTextServiceImpl.java` — implements `ISpeechToTextService`. `isAvailable()` checks `AudioSystem.getMixerInfo()`; throws `HardwareException` if no mic.

---

## 11. UI Layer (`com.umlytics.ui`)

JavaFX-only. Every UI action ends up calling a controller method — never a repository or service directly.

### 11.1 `MainWindow.java` — **the Façade**
```java
// GRASP: Facade — single access point for all controllers and UI panels
// GoF:   Facade
public class MainWindow extends Application { ... }
```
- `start(Stage)` wires the entire object graph: builds each repository → service → controller, then each panel, handing `this` (the Façade) to children so they don't see controllers directly.
- Layout: `BorderPane` with `ToolbarPanel` on top, a 3-column `SplitPane` in the middle (left = shapes + explorer, center = `DiagramEditorPanel`, right = `AIChatPanel`), status bar at the bottom.
- `static showToast(String)` — global notification channel; any panel calls `MainWindow.showToast("…")`.
- `notifyDiagramChanged()` — static observer hook used by panels that don't hold a `MainWindow` reference to trigger an explorer refresh.
- `registerShortcuts(scene)` — wires `Ctrl+N`, `Ctrl+S`, `Ctrl+Z`, `Ctrl+Shift+Z`, `Ctrl+G`.
- `updateStatusBar()` runs on the JavaFX thread to refresh project/diagram/node/zoom labels.

**Labels — and why:**
- **GoF Façade** — `MainWindow` is the *one* object the panels are given as a constructor argument. It exposes friendly methods like `openDiagram(d)`, `newDiagramForProject(id)`, `setActiveProjectId(id)`, `showToast(...)`. Behind those methods sit eight controllers, ten repositories, and three external services. Apply the test: "Does this class expose a single, simple API in front of a more complicated subsystem?" *Yes.*
- **GoF Observer (as subject)** — `notifyDiagramChanged()` is a fan-out hook: panels that don't otherwise know each other listen for "the diagram changed". The chat panel pushes a message, the explorer refreshes its tree, the editor repaints — none of them know about each other. Exactly the Observer contract.
- **GRASP Controller (at the GUI seam)** — for the keyboard shortcuts (`Ctrl+S`, `Ctrl+Z`, ...) registered in `registerShortcuts`, `MainWindow` is the first non-UI handler of the event. It then delegates to the appropriate controller.
- **GRASP Pure Fabrication** — "Main Window" is not in the domain glossary; it's a UI scaffolding class.
- **GRASP Indirection** — explicitly: panels never see `DiagramController`; they see `mainWindow.openDiagram(...)`. The Façade is the indirection.

### 11.2 `DiagramEditorPanel.java` (~2310 lines) — **the Mediator**
The biggest class. Owns:
- The `DiagramCanvas` (grid + pan + zoom).
- A `Map<ConceptualClass, ClassNode> nodeMap` linking domain to view.
- `Map<RelationshipEdge, RelationshipLink>` and `Map<RelationshipEdge, Relationship>`.
- `Deque<DiagramEdit> undoStack` + `redoStack` — full undo/redo.
- A `ToolMode` enum-state (SELECT, PAN, ADD_CLASS, ADD_RELATIONSHIP).
- A `pendingRelationshipSource` while the user shift-drags between classes.
- A `selectedClasses` set + handle map for resize gizmos.

Key responsibilities:
- `renderDiagram(UMLDiagram)` — rebuilds the visual layer from the domain object.
- `addClassNode` / `addVisualRelationship` — convert a domain element into a `ClassNode` / `RelationshipEdge` and wire interactions (click, drag, context menu).
- `configureDrag(ClassNode, ConceptualClass)` — snap-to-grid via `snap(double)`, updates relationship anchors when a node moves, refreshes selection handles.
- `updateRelationshipsForClass(ConceptualClass)` — recomputes `anchorToward(...)` for every edge attached to the moved class.
- `undo_internal()` / `redo_internal()` — pop the stack, apply the inverse `DiagramEdit`.
- `showGenerateFromTextDialog()` / `showExportDialog()` / `showUploadImageDialog()` — modal dialogs that off-load to a worker thread and `Platform.runLater(...)` the result back.
- `parseAndApplyAttributes` / `parseAndApplyMethods` — when the user inline-edits the attribute/method block, the multi-line text is re-parsed into `Attribute`/`Method` objects.
- `applyZoom(double)` — uses a `javafx.scene.transform.Scale` so the whole canvas (and its `ClassNode` children) scales uniformly; recomputes all edge endpoints.

`ClassNode` and `RelationshipEdge` never talk to each other directly — they both notify the editor panel, which decides who else cares. That is the **Mediator** pattern.

**Labels — and why:**
- **GoF Mediator (course-adjacent)** — apply the test: "Do two widgets need to react to each other, but neither holds a direct reference?" *Yes* — when a `ClassNode` is dragged, the edges connected to it must reroute. The node doesn't know which edges those are; the editor panel does. Both widgets call up into the panel, never sideways. Mediator.
- **GoF Observer (as subject of `undo/redo` notifications, as observer of `ClassNode` callbacks)** — the panel subscribes to every `ClassNode`'s `onRename`, `onDelete`, `onAttributesUpdated`, etc. (see §11.3); when the node fires a callback the panel updates the domain object and pushes a `DiagramEdit` onto the undo stack. Observer in both directions.
- **GoF Command (consumer of)** — `undo_internal()` pops a `DiagramEdit` and calls `apply(...)` on the inverse. The editor is the command *invoker*; `DiagramEdit` is the *command*.
- **GRASP Creator** — the panel "closely uses" `ClassNode`, `RelationshipEdge`, and `SelectionHandle`, so it's the natural creator. `DiagramEdit` instances are also created here because the panel has the "before" state needed to populate them.
- **GRASP Information Expert** — only the panel sees both `nodeMap` (domain → view) and `edgeMap`, so it's the right owner of `updateRelationshipsForClass(...)` and `applyZoom(...)`. No other object has that much info.

### 11.3 `ui.canvas` — visual primitives

- **`ClassNode.java`** — a `Region` with three `Label`s (title, attributes, methods). Full inline editing: double-click title → `TextField` rename; double-click attributes/methods → `TextArea` editor; context menu (Rename, Add Attribute, Add Method, Add Relationship →, Edit Class, Edit Attributes/Methods, Delete, Set Abstract/Interface, Copy, Paste). Exposes **callbacks** (`Consumer<String>`, `Runnable`, `Consumer<RelationshipType>`) for every user action — pure **Observer** semantics so the widget never imports controllers. **Why Observer?** Apply the test from §1b: "Does object A notify object B without A knowing who B is?" The node has fields like `private Consumer<String> onRename` — it doesn't know it's notifying `DiagramEditorPanel`; it just calls `onRename.accept(newName)`. The editor *subscribes* by calling `node.setOnRename(name -> ...)`. Subject/observer, decoupled exactly like the pattern requires.
- **`DiagramCanvas.java`** — extends JavaFX `Canvas`. Paints the dotted grid. Owns `zoom`, `panX`, `panY`. Re-rasterises on each drag. Wheel scroll zoom (10% per notch); Ctrl-scroll is deferred up so the editor can zoom the full scale transform.
- **`RelationshipEdge.java`** — `Group` with a `Path` (cubic Bézier), an arrow `Polygon`, a start marker `Polygon` (diamond for composition/aggregation), a `Rectangle` waypoint handle, and three `Label`s. `setRelationshipType(RelationshipType)` is the dispatch: adjusts arrowhead style (filled vs hollow), dash pattern, and start marker fill — all by reading the enum. Dragging the rectangle adjusts `bendX` which feeds back into the curvature.
- **`SelectionHandle.java`** — tiny `Rectangle` appearing at the eight grip points of a selected class.
- **`EditToolBar.java`** — toolbar with seven buttons; exposes accessors so the editor wires up handlers itself. Inversion of Control at the widget level.

### 11.4 `ui.panels` — side panels

- **`ToolbarPanel.java`** — top toolbar; one button per use case (`New Project`, `Open`, `Save`, `+ New Diagram`, `From Text`, `From Code`, `Analyse Image`, `Evaluate`, `Gen Code`, `Fit Screen`, `+/-`, `Export`). Each `onAction` is a one-liner delegating through the Façade.
- **`ShapePalettePanel.java`** — left-top sidebar. Scrollable list of draggable visuals (Class / Abstract / Interface / Enum / Note plus six relationship lines). `startDragAndDrop` puts a `SHAPE:CLASS` / `REL:ASSOCIATION` token on the clipboard; the editor's drop handler decodes it. Uses JavaFX drag-and-drop and a Prototype-style template list.
- **`ProjectExplorerPanel.java`** — left-bottom sidebar. `TreeView<Object>` showing Workspace → Projects → Diagrams + a `NewDiagramSlot` per project. Double-click handler dispatches via `instanceof` to `facade.openDiagram(d)` / `facade.newDiagramForProject(slot.projectId)` / `facade.setActiveProjectId(p.getProjectId())`. A custom `ExplorerCell` renders icons & names.
- **`AIChatPanel.java`** — right column; ChatGPT-style sidebar. `VBox` of `HBox` rows (right-aligned for `USER`, left for `AI`/`SYSTEM`) with timestamps. Three flagship actions: type a message (`send()`), `runEvaluation()` (from toolbar), `generateStructure()` (from toolbar). Each shells out to a background `Thread` and `Platform.runLater`s the result back. The "thinking" indicator is a `Timeline` cycling `● ○ ○`. Skeleton bubbles have a built-in **Copy** button that writes to the system clipboard.

### 11.5 Miscellaneous UI

- **`AppAssets.java`** — central asset loader. Reads `/assets/uml.webp` through `ImageIO` (with `SwingFXUtils.toFXImage`) and falls back to JavaFX's native loader. Returned `Image` is reused by both splash and window icon.
- **`SplashScreen.java`** — full-window animated overlay. Particle field on a `Canvas` (`AnimationTimer`), gradient wash, vignette `Rectangle`, glowing pulsing logo (`Bloom` + `DropShadow` + `RotateTransition` + `ScaleTransition` + a `Timeline` hue-shifter), typewriter credit roll for the three team members. Click anywhere to skip.
- **`ChatPanel.java`** — legacy `ListView`-based chat panel. Replaced by `AIChatPanel` but kept for tests.
- **`EvaluationPanel.java`** — older standalone evaluation pane (Evaluate / Generate Structure / Export PDF buttons, scoreboard labels, syntax-highlighted skeleton viewer). The toolbar route through `AIChatPanel` is the current path.
- **`ProjectDashboardPanel.java`** — older project list panel (New/Open/Edit/Delete/Refresh + details). Replaced by `ProjectExplorerPanel`.
- **`JavaSyntaxHighlighter.java`** — utility turning Java text into a `TextFlow` of color-coded `Text` nodes. One regex with named groups (string, comment, annotation, number, identifier, whitespace, other) and a `KEYWORDS` set. Lightweight — no RichTextFX dependency.

---

## 11b. Worked Example — How I Labeled `DiagramController`

Use this as a template for any class you have to defend in the viva. Walk through the §1b tests one row at a time and write the specific code fact next to each "yes".

| Test (from §1b) | Q | Answer for `DiagramController` | Verdict |
|---|---|---|---|
| Controller | Is this the first non-UI object handling a use-case event? | `DiagramEditorPanel.showGenerateFromTextDialog` calls `diagramCtrl.generateFromText(...)` — no repository or service is called from the UI first. | **Yes → Controller** |
| Creator | Does this class contain / closely use / have the initialising data for the created object? | Inside `evaluateDesign`, `new DesignEvaluationReport()` is `new`d up and given a `reportId`, `diagramId`, `projectId`, `evaluationDate` — the controller has all of those values right there. | **Yes → Creator** |
| Information Expert | Does it own the data needed for the responsibility? | `pruneInvalidRelationships(UMLDiagram d)` walks the diagram's `classes` and `relationships` — only the controller (after loading the diagram from the repo) has both. | **Yes → Information Expert** |
| Low Coupling | If I changed this class, would few others change? | All four collaborator fields are typed by interface (`IAIEngine`, `ICodeParser`, `IDiagramRepository`, `IExportService`). Swap any concrete class — controller doesn't change. | **Yes → Low Coupling (client of)** |
| High Cohesion | Does every method serve one responsibility? | Every method is about *a diagram use-case* — generate, modify, evaluate, export, save. Nothing about user accounts, nothing about chat. | **Yes → High Cohesion** |
| Polymorphism (do I switch on type?) | Any `instanceof`/`switch` on subclass type here? | No — relationship type dispatch is inside `Relationship.getType()`, not here. | **N/A here** |
| Pure Fabrication | Is this a real-world concept? | "Diagram controller" is a fabricated MVC role, not a domain noun. | **Yes → Pure Fabrication (mild — primary label is Controller)** |
| Indirection | Does it sit between two layers to break their coupling? | UI ↔ Controller ↔ Service/Repo. Yes. | **Yes → Indirection** |
| Protected Variations | Is it shielded by interfaces? | It depends on `IAIEngine`, `ICodeParser`, `IExportService`, `IDiagramRepository`. | **Yes (consumer of)** |
| **GoF Factory** | Does any method return a fully-built object of an abstract/typed result? | `generateFromText` returns a `UMLDiagram` (and assigns its UUID), but the actual `new UMLDiagram()` happens *inside* `UMLModel.toUMLDiagram()`. So the Factory Method lives on `UMLModel`, not on the controller. | **No (the controller is the *client* of a Factory, not the factory itself)** |
| **GoF Singleton** | Private constructor + static getInstance? | No — `new DiagramController(...)` in `MainWindow.start`. | **No** |
| **GoF Adapter** | Does it translate an external API to our internal API? | No — its collaborators do that. | **No (consumer of Adapters)** |
| **GoF Façade** | Does it expose a simple API in front of a more complex subsystem? | Yes — `generateFromText(...)` hides validation + AI HTTP + JSON parsing + domain hydration + repository transaction. One method, six collaborators. | **Yes → Façade** |
| **GoF Observer** | Does it notify listeners on state change without knowing them? | No — it's strictly synchronous request/response. | **No** |
| **GoF Strategy (client of)** | Does it depend on interfaces with multiple interchangeable impls? | Yes — every field is an interface (`IAIEngine`, `ICodeParser`, `IExportService`, `IDiagramRepository`). | **Yes (client of Strategy)** |

That table is exactly what produced the labels at the top of §3.1. Repeat the same process for any other class you need to defend.

> **Defence pro-tip:** if an examiner says "but isn't `DiagramController` *also* a Mediator?", check the test: "do two unrelated objects need to talk to each other through it?" Here, the AI engine and the repository don't need to talk to each other — they each do their job independently and the controller chains them. That's Façade (one entry point), not Mediator (peer-to-peer brokerage). Always go back to the test, not the vibe.

---

## 12. GRASP — Full Map

GRASP is a set of nine responsibility-assignment principles. Here is where each appears in UMLytics.

| Principle | Where in UMLytics | Why it fits |
|---|---|---|
| **Information Expert** | `UMLDiagram`, `ConceptualClass`, `DiagramEdit`, `DiagramController` | Each owns the data needed to fulfil its responsibility. `UMLDiagram` knows how to add/remove its own classes and stamp `lastModifiedDate`. `DiagramController` owns diagram invariants via `pruneInvalidRelationships`. |
| **Creator** | `DiagramController` (creates `DesignEvaluationReport`, `ClassSuggestion`); `UMLModel.toUMLDiagram()`; `DiagramEditorPanel` (creates `ClassNode`, `RelationshipEdge`); `JavaCodeParser` (creates `ConceptualClass`, `Attribute`, `Method`, edges) | Each creator "closely uses", "contains", or "records" the created object — the principle's exact criteria. |
| **Controller** | `DiagramController`, `AIController`, `ProjectController`; `MainWindow` and `ToolbarPanel` at the GUI seam | They are the only objects the UI invokes for use cases. Zero UI code, zero SQL — pure orchestration. |
| **Low Coupling** | The interface package (`IAIEngine`, `ICodeParser`, `IDiagramRepository`, `IExportService`, `IChatRepository`, `IEvaluationRepository`, `IProjectRepository`, `ISpeechToTextService`) | Controllers depend on abstractions. Replace `LLMAPIEngine` with a mock and the tests still pass. |
| **High Cohesion** | Each repository persists one aggregate; each enum has a single classification axis; each domain class collects only related fields | If you merged `ChatRepositoryImpl` and `DiagramRepositoryImpl`, the resulting class would have two responsibilities — that's the cohesion smell test. |
| **Polymorphism** | `Relationship` hierarchy (`Association`/`Inheritance`/`Dependency`); `IAIEngine` family; `IExportService` family | `Relationship.getType()` returns the right enum based on the subclass; export/AI calls dispatch through the interface without `instanceof`. |
| **Pure Fabrication** | `DatabaseManager`, `ConnectionPool`, all `…RepositoryImpl`, `LLMAPIEngine`, `JavaCodeParser`, `DiagramExportService`, `SpeechToTextServiceImpl`, `AiDiagramPayload`, `SkeletonResponseNormalizer` | None represent a real-world concept from requirements. They exist solely to keep the domain clean. |
| **Indirection** | The interface package + the Façade `MainWindow` | UI never touches a repository directly; controllers never touch JDBC directly. Each indirection removes a coupling. |
| **Protected Variations** | Interfaces are the stable surface; concretes vary behind them (OpenAI vs Gemini in `LLMAPIEngine`; PNG/PDF/SVG in `DiagramExportService`). `DatabaseManager.applySchemaMigrations` shields callers from legacy table names | Future changes (new AI provider, new export format) are local. |

---

## 13. GoF — Six Patterns We Covered

| Category | Pattern | Used in UMLytics |
|---|---|---|
| **Creational** | Factory, Singleton | ✅ ✅ |
| **Structural** | Adapter, Façade | ✅ ✅ |
| **Behavioral** | Observer, Strategy | ✅ ✅ |

### 13.1 Creational — **Factory**

**Intent:** Provide a method that returns a fully-constructed object, hiding the concrete class.

**Where in UMLytics:**

#### A. `UMLModel.toUMLDiagram()` — Factory Method on a transient DTO
```java
public UMLDiagram toUMLDiagram() {
    UMLDiagram diagram = new UMLDiagram();
    diagram.setDiagramId(UUID.randomUUID());
    for (ConceptualClass c : classes) diagram.addConceptualClass(c);
    for (Relationship r : relationships) diagram.addRelationship(r);
    return diagram;
}
```
The caller never news up a `UMLDiagram` and never assigns its UUID — the factory handles initialisation. Every AI generation path (`DiagramController.generateFromText`, `generateFromCode`, `analyzeUploadedImage`) ends with `model.toUMLDiagram()`.

#### B. `Project.createProject(name, desc)` — Factory Method on the aggregate
Initialises `name`, `description`, `createdDate`, `lastModifiedDate`, and `projectId` in one call. `ProjectController.createProject` uses it after validation.

#### C. `LLMAPIEngine.buildRelationship(RelationshipType)` — Factory Method picking the right subclass
```java
private static Relationship buildRelationship(RelationshipType rt) {
    switch (rt) {
        case COMPOSITION:  AssociationRelationship a = new AssociationRelationship(); a.setComposition(true); return a;
        case AGGREGATION:  AssociationRelationship a = new AssociationRelationship(); a.setAggregation(true); return a;
        case ASSOCIATION:  return new AssociationRelationship();
        case INHERITANCE:  return new InheritanceRelationship();
        case REALIZATION:  InheritanceRelationship i = new InheritanceRelationship(); i.setInterface(true); i.setDashed(true); return i;
        case DEPENDENCY:   DependencyRelationship d = new DependencyRelationship(); d.setDashed(true); return d;
        default:           return new AssociationRelationship();
    }
}
```
The caller wants "a relationship" — the factory decides which concrete subclass and configures its discriminator flags.

#### D. `DiagramEdit.apply(UMLDiagram)` — factory for `ConceptualClass` in the `ADD_CLASS` case
Acts as a small factory for the new class created when an undo entry is re-applied.

**Why we use Factory here:**
- The "create an object" step is non-trivial (must set UUIDs, defaults, sibling flags).
- The exact concrete type depends on a discriminator value (`RelationshipType`).
- Callers should not depend on the concrete classes directly — they go through a factory method that returns the abstract type.

### 13.2 Creational — **Singleton**

**Intent:** Ensure a class has exactly one instance, and provide a global access point.

**Where in UMLytics:**

#### `DatabaseManager.java`
```java
public class DatabaseManager {
    private static DatabaseManager instance;
    private final ConnectionPool connectionPool;
    private String jdbcUrl, username, password;

    private DatabaseManager() {                // private constructor
        this.connectionPool = new ConnectionPool();
        loadConfig();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }
}
```
- **Private constructor** — no one else can instantiate.
- **Static `getInstance()` synchronized** — thread-safe lazy initialisation.
- Holds **one** `ConnectionPool` and **one** config tuple for the whole app.
- Every repository (`ProjectRepositoryImpl`, `DiagramRepositoryImpl`, `ChatRepositoryImpl`, `DesignEvaluationRepositoryImpl`) uses `DatabaseManager.getInstance().getConnection()`.

**Why Singleton here:**
- The JDBC URL and the connection pool are global resources — you don't want two pools fighting for the same SQLite file.
- The schema initialisation (`initialize()`) must run exactly once at app start.

### 13.3 Structural — **Adapter**

**Intent:** Convert the interface of an external class into the interface our system expects.

**Where in UMLytics:**

#### A. `LLMAPIEngine` adapts HTTP-JSON LLM APIs to `IAIEngine`
Outside the class: a plain Java method `generateFromText(String desc) → UMLModel`.
Inside the class: builds OkHttp `Request`s with JSON bodies, calls OpenAI/Cerebras/Gemini, parses chat completions, hydrates domain objects.
The rest of the system has no idea HTTP exists.

#### B. `LLMAPIEngine.convertOpenAiChatRequestToGemini(JsonObject)` — Adapter inside the Adapter
OpenAI uses `{ "model", "messages", "response_format" }`.
Gemini uses `{ "generationConfig", "systemInstruction", "contents" }`.
This method walks the OpenAI request and emits the Gemini equivalent. Same code path, two providers.

#### C. `JavaCodeParser` adapts the external `com.github.javaparser` library to `ICodeParser`
We don't want our controllers to import `CompilationUnit`, `ClassOrInterfaceDeclaration`, `RecordDeclaration`. `JavaCodeParser` translates those AST nodes into our domain `ConceptualClass`, `Attribute`, `Method`, and `Relationship` objects.

#### D. `SkeletonResponseNormalizer.toCleanSkeletonsJson(...)` — Adapter for varied LLM JSON shapes
Different LLMs return wildly different JSON shapes for "give me Java skeletons":
- OpenAI: `{"skeletons":{"Foo":"public class Foo {...}"}}`
- Gemini in json-schema mode: `{"properties":{"skeletons":{"properties":{...}}}}`
- Some models nest Java inside `{"source":"...", "code":"..."}`

The normalizer adapts all of them to the single canonical shape the UI expects.

**Why Adapter here:**
- Third-party APIs change shape; we keep our domain interface stable.
- Multiple external providers (OpenAI, Cerebras, Gemini) all serve the same `IAIEngine` interface.

### 13.4 Structural — **Façade**

**Intent:** Provide a unified, simplified interface to a set of subsystems.

**Where in UMLytics:**

#### A. `MainWindow.java` — **the textbook UI Façade**
```java
// GRASP: Facade — single access point for all controllers and UI panels
// GoF:   Facade
public class MainWindow extends Application {
    private ProjectController projectCtrl;
    private DiagramController diagramCtrl;
    private AIController      aiCtrl;
    private ToolbarPanel toolbarPanel;
    private ShapePalettePanel shapePanel;
    private ProjectExplorerPanel explorerPanel;
    private DiagramEditorPanel editorPanel;
    private AIChatPanel chatPanel;
    ...
    public void promptNewProject() { ... }
    public void openDiagram(UMLDiagram d) { ... }
    public void newDiagramForProject(UUID projectId) { ... }
    public void refreshProjectExplorer() { ... }
    public static void showToast(String msg) { ... }
}
```
- The toolbar, the chat panel, the explorer, and the editor all receive **one** reference: the `MainWindow` façade.
- They never see `ProjectController`, `DiagramController`, `AIController` directly — they call `facade.getProjectController()` / `facade.getEditorPanel().showGenerateFromTextDialog()` and similar methods.
- New behaviour added to a controller is exposed via one new method on the façade. The panels stay simple.

#### B. `DiagramController` and `AIController` — domain-side Façades
A single typed call like `diagramCtrl.generateFromText(projectId, desc)` hides:
- input validation
- the AI engine HTTP call
- JSON parsing and domain hydration
- diagram tagging with `SourceType`
- transactional save to SQLite
- multi-table writes via the repository

That's a subsystem of ~6 collaborating classes, exposed through one method. Pure Façade.

**Why Façade here:**
- The UI is decoupled from controllers via one stable surface (`MainWindow`).
- Controllers decouple the UI from the AI + Repo + Parser + Export subsystems.
- New components plug in behind the façades without rippling changes.

### 13.5 Behavioral — **Observer**

**Intent:** Define a one-to-many dependency so that when one object changes state, all its dependents are notified.

**Where in UMLytics:**

#### A. `ClassNode` — callback observers for every user gesture
```java
public class ClassNode extends Region {
    private Consumer<String>            renameHandler;
    private Runnable                    addAttributeHandler;
    private Runnable                    addMethodHandler;
    private Runnable                    editClassHandler;
    private Consumer<RelationshipType>  addRelationshipHandler;
    private Runnable                    deleteHandler;
    private Consumer<Boolean>           toggleAbstractHandler;
    private Consumer<Boolean>           toggleInterfaceHandler;
    private Runnable                    copyHandler;
    private Runnable                    pasteHandler;
    private Consumer<String>            editAttributesTextHandler;
    private Consumer<String>            editMethodsTextHandler;
    ...
}
```
The `ClassNode` widget never imports any controller. Instead it exposes setters (`setRenameHandler`, `setAddAttributeHandler`, …). The `DiagramEditorPanel` subscribes by registering its own lambdas. When the user clicks Rename in the context menu, `ClassNode` *notifies its observer* via `renameHandler.accept(newName)`. The widget pushes change events; the panel listens.

#### B. JavaFX event handlers — Observer baked into the framework
Every `setOnAction`, `setOnMouseClicked`, `setOnDragDetected`, `setOnDragOver`, `setOnContextMenuRequested` is an Observer registration. The button does not know who's listening; the controller registers a lambda and gets notified when the button fires.

#### C. `MainWindow.notifyDiagramChanged()` — application-level Observer
```java
public static void notifyDiagramChanged() {
    MainWindow w = instance;
    if (w != null) {
        Platform.runLater(w::refreshProjectExplorer);
    }
}
```
Panels that don't hold a `MainWindow` reference (e.g. inside async threads) call this static method to notify the Façade that the persisted set of diagrams has changed. The Façade reacts by reloading the explorer tree.

#### D. `RelationshipEdge.setBendChangedHandler(DoubleConsumer)`
The edge widget notifies its observer (the editor) whenever the user drags the curvature handle. The editor records a `DiagramEdit` and pushes onto the undo stack.

**Why Observer here:**
- UI widgets must stay reusable — they can't depend on a specific controller.
- One state change (user clicks Save) needs to notify many listeners (status bar, project tree, toast system).

### 13.6 Behavioral — **Strategy**

**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable at runtime.

**Where in UMLytics:**

#### A. `IAIEngine` — Strategy family for AI providers
```java
public interface IAIEngine {
    UMLModel generateFromText(String desc);
    DesignEvaluationReport evaluateDesign(UMLModel m);
    String consultDesign(String q, ProjectContext ctx);
    String generateStructure(UMLModel m);
    UMLModel analyzeImage(byte[] data);
}
```
- The controller holds an `IAIEngine` field — it doesn't know whether the strategy talks to OpenAI, Cerebras, or Gemini.
- The current implementation `LLMAPIEngine` switches *internally* between OpenAI-compatible and Gemini protocols based on config, but a separate implementation (say `AnthropicAIEngine`, `OfflineMockAIEngine`) could be dropped in without touching any controller code.

#### B. `IExportService` — Strategy family for output formats
```java
public interface IExportService {
    void export(UMLDiagram d, ExportFormat fmt, String path);
    List<ExportFormat> getSupportedFormats();
}
```
Implemented by `DiagramExportService`, which dispatches internally via:
```java
switch (fmt) {
    case PNG -> bytes = renderToPNG(d);
    case PDF -> bytes = renderToPDF(d);
    case SVG -> bytes = renderToSVG(d);
}
```
Each branch is a separate **strategy** (AWT/ImageIO, PDFBox, Batik). The controller picks one at runtime via the user's choice in the export dialog.

#### C. `ICodeParser` — Strategy seam for source languages
Today only `JavaCodeParser` is implemented; the seam exists so a future `PythonCodeParser` could be wired in without rewriting `DiagramController`.

#### D. `ISpeechToTextService` — Strategy seam for STT vendors
Same idea — `SpeechToTextServiceImpl` is the current strategy; could be swapped for a Whisper-API-based one.

**Why Strategy here:**
- The exact algorithm/provider changes; the calling code shouldn't.
- New variants are added by writing a new implementation, not by editing `if/else` ladders in the caller.

---

## 14. End-to-End Use-Case Walkthrough

Take **"Generate diagram from text"** (UC2 in the SDA report). Every layer cooperates:

1. **UI** — User presses `Ctrl+G` or clicks **From Text** in `ToolbarPanel`.
   → `MainWindow.registerShortcuts` or `btnText.setOnAction` triggers `editorPanel.showGenerateFromTextDialog()`.

2. **UI** — `DiagramEditorPanel.showGenerateFromTextDialog()` opens a `TextInputDialog`, then on a worker `Thread` calls `diagramCtrl.generateFromText(projectId, desc)`.

3. **Controller** — `DiagramController.generateFromText(...)`
   - Validates length (throws `ValidationException` if < 10 chars).
   - Calls `aiEngine.generateFromText(desc)` (**Strategy**).

4. **Service / Adapter** — `LLMAPIEngine.generateFromText(...)`
   - Builds JSON via `buildUmlChatBody(...)` with `SYSTEM_UML_JSON`.
   - Calls OpenAI or Gemini via `postChatCompletions(...)`.
   - `parseUmlModelFromJson(...)` rehydrates a `UMLModel`, creating `ConceptualClass` / `Attribute` / `Method` and the right `Relationship` subclass via **Factory** (`buildRelationship`).

5. **Controller** — calls `model.toUMLDiagram()` (**Factory Method**), sets `SourceType.NATURAL_LANGUAGE`, then `diagramRepo.save(diagram)`.

6. **Repository** — `DiagramRepositoryImpl.save(...)`
   - `conn.setAutoCommit(false)` → `insertDiagramRow` → `insertDiagramContent` (inserts classes, attributes, methods, relationships).
   - Uses `DatabaseManager.getInstance().getConnection()` (**Singleton**).

7. **UI** — worker thread `Platform.runLater(...)` switches back to the FX thread; `renderDiagram(d)` rebuilds the canvas (`ClassNode` + `RelationshipEdge` per element); `MainWindow.notifyDiagramChanged()` triggers the explorer refresh (**Observer**); `showToast("Diagram generated ✓")` confirms.

Six patterns, three layers, one feature.

---

## Quick Reference Card

| Pattern | Class / Method | One-line role |
|---|---|---|
| **Factory** | `UMLModel.toUMLDiagram()` | Builds a fully-initialised `UMLDiagram` |
| **Factory** | `Project.createProject(name, desc)` | Initialises a fresh `Project` |
| **Factory** | `LLMAPIEngine.buildRelationship(RelationshipType)` | Picks the right `Relationship` subclass |
| **Singleton** | `DatabaseManager.getInstance()` | Single JDBC/connection-pool owner |
| **Adapter** | `LLMAPIEngine` | HTTP-JSON LLM ↔ `IAIEngine` |
| **Adapter** | `LLMAPIEngine.convertOpenAiChatRequestToGemini` | OpenAI shape ↔ Gemini shape |
| **Adapter** | `JavaCodeParser` | JavaParser AST ↔ domain |
| **Adapter** | `SkeletonResponseNormalizer` | Varied LLM JSON ↔ canonical skeletons |
| **Façade** | `MainWindow` | One surface for the whole UI |
| **Façade** | `DiagramController` / `AIController` / `ProjectController` | One surface for each use-case group |
| **Observer** | `ClassNode.setRenameHandler(...)` etc. | Widget pushes change to editor |
| **Observer** | JavaFX `setOn*` handlers | Standard event subscription |
| **Observer** | `MainWindow.notifyDiagramChanged()` | App-level state-change broadcast |
| **Strategy** | `IAIEngine` (impl: `LLMAPIEngine`) | Swap AI providers |
| **Strategy** | `IExportService` (impl: `DiagramExportService`) | Swap output format |
| **Strategy** | `ICodeParser` (impl: `JavaCodeParser`) | Swap source-language parser |
| **Strategy** | `ISpeechToTextService` (impl: `SpeechToTextServiceImpl`) | Swap STT vendor |

---

*End of masterclass — UMLytics codebase, mapped to GRASP and to the six GoF patterns covered in the course.*
