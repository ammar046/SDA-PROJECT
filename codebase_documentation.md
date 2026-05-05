# IN-DEPTH UMLYTICS CODEBASE DOCUMENTATION
*Mapped strictly to the `src/main/java/com/umlytics` physical codebase*

This document provides a class-by-class, file-by-file intensive breakdown of the UMLytics application. It proves full compliance with GRASP principles, GoF patterns, and Layered Architecture as requested by the SDA assignments.

---

## 1. APPLICATION ENTRY
### `Main.java`
*   **Purpose**: The JavaFX Application Bootstrapper.
*   **Key Behavior**: Contains only the `public static void main(String[] args)` method. It calls `Application.launch(MainWindow.class, args)` to bypass Java 9+ Module path restrictions and initialize the primary UI thread.

---

## 2. CONTROLLER LAYER (`com.umlytics.controllers`)
*Enforces the **GRASP Controller** pattern. These classes hold zero UI code and zero SQL queries, strictly mediating between the UI panels and the backend Repositories/Services.*

### `DiagramController.java`
*   **Purpose**: Central boundary for manipulating and generating `UMLDiagram` objects.
*   **Design Pattern**: Controller (GRASP), Creator.
*   **Key Methods**:
    *   `generateFromText(UUID, String)`: Takes Natural Language (NL), validates length (>10 chars), triggers `IAIEngine`, and persists the returned `UMLModel`.
    *   `generateFromCode(UUID, List<File>)`: Takes `.java` files, delegates to `ICodeParser`, and persists the resulting diagram.
    *   `modifyClassDefinition(UUID, DiagramEdit)`: Receives a transformational `DiagramEdit` object, applies it to the target diagram, calls `pruneInvalidRelationships()` to ensure structural integrity (no orphan relationships), and updates the DB.
    *   `analyzeUploadedImage(UUID, byte[])`: Validates PNG/JPG chunk headers (magic bytes) to ensure file integrity before passing to the Vision AI.
    *   `exportDiagram(UUID, ExportFormat, String)`: Routes the diagram to the `IExportService`.

### `AIController.java`
*   **Purpose**: Handles AI co-pilot and NLP interactions. 
*   **Design Pattern**: Controller (GRASP).
*   **Key Methods**:
    *   `evaluateDesign(UUID)`: Fetches a diagram, checks if it has `< 2 classes` (throws `DiagramTooSimpleException`), converts it to `UMLModel`, and generates a `DesignEvaluationReport`.
    *   `submitDesignQuestion(String, UUID, UUID)`: Constructs a `ProjectContext` containing chat history and current canvas state, routes via `IAIEngine`, and saves both user and AI `ChatMessage` objects to the repository.
    *   `generateStructureSuggestions(UUID)`: Prompts the LLM to generate Java skeleton code for the current diagram, returning a `ClassSuggestion`.

### `ProjectController.java`
*   **Purpose**: Manages the overarching `Project` entities (Workspaces).
*   **Key Methods**: `createProject`, `openProject`, `updateProjectMetadata`. Highly reliant on `ValidationException` to prevent duplicate workspace names.

---

## 3. DOMAIN MODEL (`com.umlytics.domain`)
*The central enterprise objects.*

### `DiagramEdit.java`
*   **Purpose**: Represents a single atomic canvas manipulation (an Undo/Redo frame).
*   **Key Logic**: The `apply(UMLDiagram)` method features a dense `switch(editType)`. If `ADD_CLASS`, it instantiates a `ConceptualClass` via UUID; if `REMOVE_CLASS`, it drops it; if `RENAME_CLASS`, it iterates over the diagram's classes and mutates the matching ID string.

### `UMLDiagram.java` & `UMLModel.java`
*   **Purpose**: `UMLDiagram` tracks metadata (ProjectID, SourceType) and aggregates `ConceptualClass` and `Relationship` lists. `UMLModel` is a transient wrapper used specifically to serialize the diagram into JSON for the LLM.

### Diagram Components: `ConceptualClass`, `Attribute`, `Method`
*   **Purpose**: The actual logical nodes. A `ConceptualClass` holds `List<Attribute>` and `List<Method>`, determining if it is an Interface or Abstract class.

### Line Routing: `Relationship.java`, `InheritanceRelationship.java`, etc.
*   **Purpose**: Employs **Polymorphism**. The base abstract `Relationship` is extended by exact line behaviors (Association, Dependency, Inheritance), which dictates source/target multiplicities and line styling without cluttered if-statements.

### AI Context wrappers: `ClassSuggestion`, `DesignEvaluationReport`, `ProjectContext`, `ChatMessage`
*   **Purpose**: Models the responses returning from the Anthropic/OpenAI APIs. Examples include storing Cohesion/Coupling scores (`DesignEvaluationReport`) or String payloads of code (`ClassSuggestion`).

---

## 4. DATABASE & REPOSITORIES (`com.umlytics.repository` & `db`)
*Enforces GoF **Repository** and GRASP **Pure Fabrication**.*

### `db/DatabaseManager.java` & `db/ConnectionPool.java`
*   **Purpose**: The central SQLite JDBC hub. `DatabaseManager` is a strict Singleton that builds schemas from `schema.sql`. `ConnectionPool` explicitly prevents threading blocks when multiple JavaFX elements query simultaneously.

### `DiagramRepositoryImpl.java`
*   **Purpose**: The most complex repository. Serializes lists of `ConceptualClass` and `Relationship` objects into relational SQL `INSERT`/`UPDATE` operations using `PreparedStatement` mappings to prevent SQL injection.

### `ProjectRepositoryImpl.java`, `ChatRepositoryImpl.java`, `DesignEvaluationRepositoryImpl.java`
*   **Purpose**: Isolates raw JDBC from the controllers. Maps generic CRUD operations (Save, FindById, Delete).

---

## 5. EXTERNAL SERVICES (`com.umlytics.services`)
### `LLMAPIEngine.java`
*   **Purpose**: Constructs raw HTTP JSON payloads via OkHttp to invoke NLP requests. Takes `ProjectContext` to prepend previous messages.

### `JavaCodeParser.java`
*   **Purpose**: Wraps the external `com.github.javaparser` library. Traverses Java Abstract Syntax Trees (AST) and maps `ClassOrInterfaceDeclaration` nodes to domain `ConceptualClass` objects.

### `DiagramExportService.java`
*   **Purpose**: Employs layout algorithms and Apache PDFBox/Batik to turn the in-memory Canvas into binary PNG/SVG/PDF files.

---

## 6. EXCEPTIONS (`com.umlytics.exceptions`)
*Prevents backend failures from propagating unhandled into the UI thread.*

*   **Logic Errors**: `EmptyDiagramException`, `DiagramTooSimpleException`, `ValidationException`. Thrown directly by Controllers when user inputs violate Use Case edge paths.
*   **Infra Errors**: `DatabaseException`, `HardwareException`, `SpeechServiceException`, `AIEngineException`.

---

## 7. UI PRESENTATION LAYER (`com.umlytics.ui`)
*Enforces GoF Facade. Contains all JavaFX imports.*

### Shell & Context
*   **`MainWindow.java`**: The **Facade**. Bootstraps the CSS, the JavaFX `BorderPane`, assembles the split panels, and maps keyboard accelerators (Ctrl+S, Ctrl+Z). Sets up a unified Toast notification overlay system `showToast()`.

### Panels (`ui/panels`)
*   **`DiagramEditorPanel.java`**: The absolute behemoth of the system. Manages the visual Undo/Redo stack, intercepts keyboard events, and routes visual canvas clicks into `DiagramEdit` operations via the `DiagramController`.
*   **`AIChatPanel.java`**: The ChatGPT-like sidebar. Displays `SenderType.USER` on the right and `SenderType.AI` on the left using visual HBoxes.
*   **`ShapePalettePanel.java` & `ProjectExplorerPanel.java`**: Side navigational drawers. Project Explorer is a native `TreeView`.

### Canvas Drawing Shapes (`ui/canvas`)
*   **`ClassNode.java`**: The JavaFX visual component combining a title background, a separator, and `Text` fields spanning Attributes and Methods.
*   **`RelationshipEdge.java`**: Mathematical `Path` or `Line` elements acting as edges capable of locking onto `ClassNode` coordinate bounds.
*   **`SelectionHandle.java` & `EditToolBar.java`**: Drag-and-drop bounding boxes and context-menus that pop up over actively clicked shapes.
