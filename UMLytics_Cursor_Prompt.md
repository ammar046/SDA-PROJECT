# UMLytics — Comprehensive Cursor Implementation Prompt

> **For:** Cursor AI  
> **Project:** UMLytics — AI-Powered UML Generation and Design Intelligence Tool  
> **Team:** Ammar Bin Omer (24I-0500), Haris Zahid (24I-0643), Shahmeer Jadoon (24I-0879)  
> **Stack:** JavaFX 17+ (replaces Swing references in class diagram for superior canvas rendering), Maven, SQLite/PostgreSQL via JDBC, Anthropic/OpenAI LLM API

---

## 0. Pre-Implementation Mandate

Before writing a single line of code, internalize these hard rules:

1. **The class diagram is the source of truth.** Every class, interface, field, method signature, enum value, and relationship shown in the D5 class diagram must exist verbatim in the implementation. Do not rename, merge, omit, or add entities unless the rules below explicitly allow it.
2. **JavaFX replaces Swing.** The class diagram says `UI LAYER (Java Swing)` and uses `JTextArea`, `JList`. Use JavaFX equivalents (`TextArea`, `ListView<T>`) throughout. This is the only deviation allowed from the diagram — it is mandated by the requirement that the diagram editor is an **exact draw.io replica**, which is impossible in raw Swing.
3. **Consistency across all deliverables.** Every Use Case (UC1–UC12), every SSD flow (SSD1–SSD12), and every domain concept from the Domain Model must be traceable to code.
4. **draw.io canvas is non-negotiable.** `DiagramEditorPanel` must render a pixel-faithful recreation of draw.io's interface. Specifics are in Section 8.
5. **No shortcuts.** Implement every extension scenario, every error path, every database rollback described in the use cases.

---

## 1. Project Structure

```
UMLytics/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/umlytics/
│       │       ├── Main.java                          # JavaFX Application entry point
│       │       ├── interfaces/                        # All <<interface>> types
│       │       │   ├── IDiagramRepository.java
│       │       │   ├── IProjectRepository.java
│       │       │   ├── IAIEngine.java
│       │       │   ├── ISpeechToTextService.java
│       │       │   ├── IExportService.java
│       │       │   ├── ICodeParser.java
│       │       │   ├── IChatRepository.java
│       │       │   └── IEvaluationRepository.java
│       │       ├── domain/                            # Domain model entities
│       │       │   ├── Project.java
│       │       │   ├── UMLDiagram.java
│       │       │   ├── UMLClass.java
│       │       │   ├── Attribute.java
│       │       │   ├── Method.java
│       │       │   ├── Relationship.java
│       │       │   ├── InheritanceRelationship.java
│       │       │   ├── AssociationRelationship.java
│       │       │   ├── DependencyRelationship.java
│       │       │   ├── ChatMessage.java
│       │       │   ├── EvaluationReport.java
│       │       │   ├── StructureSuggestion.java
│       │       │   ├── UMLModel.java
│       │       │   ├── ProjectContext.java
│       │       │   └── DiagramEdit.java
│       │       ├── enums/                             # All <<enumeration>> types
│       │       │   ├── Visibility.java
│       │       │   ├── RelationshipType.java
│       │       │   ├── SenderType.java
│       │       │   ├── SourceType.java
│       │       │   ├── ExportFormat.java
│       │       │   ├── EditType.java
│       │       │   └── Navigability.java
│       │       ├── controllers/                       # GRASP Controllers
│       │       │   ├── ProjectController.java
│       │       │   ├── DiagramController.java
│       │       │   └── AIController.java
│       │       ├── repository/                        # Repository implementations
│       │       │   ├── ProjectRepositoryImpl.java
│       │       │   ├── DiagramRepositoryImpl.java
│       │       │   ├── ChatRepositoryImpl.java
│       │       │   └── EvaluationRepositoryImpl.java
│       │       ├── services/                          # Service implementations
│       │       │   ├── LLMAPIEngine.java
│       │       │   ├── JavaCodeParser.java
│       │       │   ├── DiagramExportService.java
│       │       │   └── SpeechToTextServiceImpl.java
│       │       ├── db/
│       │       │   └── DatabaseManager.java
│       │       └── ui/                                # JavaFX UI Layer
│       │           ├── MainWindow.java
│       │           ├── ProjectDashboardPanel.java
│       │           ├── DiagramEditorPanel.java
│       │           ├── ChatPanel.java
│       │           ├── EvaluationPanel.java
│       │           └── canvas/
│       │               ├── DiagramCanvas.java
│       │               ├── ClassNode.java
│       │               ├── RelationshipEdge.java
│       │               ├── SelectionHandle.java
│       │               └── EditToolBar.java
│       └── resources/
│           ├── css/
│           │   └── drawio-theme.css
│           ├── schema.sql
│           └── config.properties
```

---

## 2. Maven `pom.xml` Dependencies

Include these in `pom.xml`:

```xml
<dependencies>
  <!-- JavaFX -->
  <dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
  </dependency>
  <dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21</version>
  </dependency>
  <dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-swing</artifactId>
    <version>21</version>
  </dependency>

  <!-- Database -->
  <dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
  </dependency>

  <!-- HTTP Client for LLM API -->
  <dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
  </dependency>

  <!-- JSON parsing -->
  <dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
  </dependency>

  <!-- Java parser for source code analysis -->
  <dependency>
    <groupId>com.github.javaparser</groupId>
    <artifactId>javaparser-core</artifactId>
    <version>3.25.8</version>
  </dependency>

  <!-- PDF export -->
  <dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
  </dependency>

  <!-- SVG export via Batik -->
  <dependency>
    <groupId>org.apache.xmlgraphics</groupId>
    <artifactId>batik-svggen</artifactId>
    <version>1.17</version>
  </dependency>

  <!-- Speech-to-text (optional, Vosk offline) -->
  <dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.14.0</version>
  </dependency>
</dependencies>
```

---

## 3. Enumerations (Section `enums/`)

Implement exactly as shown in the class diagram:

### `Visibility.java`
```java
public enum Visibility { PUBLIC, PRIVATE, PROTECTED }
```

### `RelationshipType.java`
```java
public enum RelationshipType { ASSOCIATION, INHERITANCE, DEPENDENCY, COMPOSITION, AGGREGATION }
```

### `SenderType.java`
```java
public enum SenderType { USER, AI, SYSTEM }
```

### `SourceType.java`
```java
public enum SourceType { NATURAL_LANGUAGE, SOURCE_CODE, UPLOADED }
```

### `ExportFormat.java`
```java
public enum ExportFormat { PNG, PDF, SVG }
```

### `EditType.java`
```java
public enum EditType { ADD_CLASS, REMOVE_CLASS, RENAME, ADD_ATTR, REMOVE_ATTR, ADD_METHOD, REMOVE_METHOD, CHANGE_RELATION }
```

### `Navigability.java`
```java
public enum Navigability { IN, OUT, BOTH, NONE }
```

---

## 4. Interfaces (Section `interfaces/`)

Implement exactly as shown. All methods must match the diagram's signatures precisely.

### `IDiagramRepository.java`
```java
public interface IDiagramRepository {
    void save(UMLDiagram d);
    UMLDiagram findById(int id);
    List<UMLDiagram> findByProject(int pid);
    void delete(int id);
    void update(UMLDiagram d);
}
```

### `IProjectRepository.java`
```java
public interface IProjectRepository {
    void save(Project p);
    Project findById(int id);
    List<Project> findAll();
    void delete(int id);
    void update(Project p);
}
```

### `IAIEngine.java`
```java
public interface IAIEngine {
    UMLModel generateFromText(String desc);
    EvaluationReport evaluateDesign(UMLModel m);
    String consultDesign(String q, ProjectContext ctx);
    String generateStructure(UMLModel m);
    UMLModel analyzeImage(byte[] data);
}
```

### `ISpeechToTextService.java`
```java
public interface ISpeechToTextService {
    void startRecording();
    String stopRecording();
    boolean isAvailable();
}
```

### `IExportService.java`
```java
public interface IExportService {
    void export(UMLDiagram d, ExportFormat fmt, String path);
    List<ExportFormat> getSupportedFormats();
}
```

### `ICodeParser.java`
```java
public interface ICodeParser {
    UMLModel parse(List<File> files);
    List<String> getSupportedLanguages();
}
```

### `IChatRepository.java`
```java
public interface IChatRepository {
    void save(ChatMessage m);
    List<ChatMessage> findByProject(int pid);
    void delete(int id);
}
```

### `IEvaluationRepository.java`
```java
public interface IEvaluationRepository {
    void save(EvaluationReport r);
    List<EvaluationReport> findByProject(int pid);
    EvaluationReport findByDiagram(int did);
    void delete(int id);
}
```

---

## 5. Domain Model Classes (Section `domain/`)

All fields and methods must be exactly as in the class diagram.

### `Project.java`
```java
public class Project {
    private int projectId;
    private String name;
    private String description;
    private Date createdDate;
    private Date lastModifiedDate;
    // Constructor, getters, setters
}
```

### `UMLDiagram.java`
```java
public class UMLDiagram {
    private int diagramId;
    private String title;
    private Date createdDate;
    private Date lastModifiedDate;
    private SourceType sourceType;
    // Constructor, getters, setters
}
```

### `UMLClass.java`
```java
public class UMLClass {
    private int classId;
    private String name;
    private boolean isAbstract;
    private boolean isInterface;
    private double positionX;
    private double positionY;
    private List<Attribute> attributes = new ArrayList<>();
    private List<Method> methods = new ArrayList<>();

    public void addAttribute(Attribute a);
    public void removeAttribute(int id);
    public void addMethod(Method m);
    public void removeMethod(int id);
    public List<Attribute> getAttributes();
    public List<Method> getMethods();
}
```

### `Attribute.java`
```java
public class Attribute {
    private int attributeId;
    private String name;
    private String type;
    private Visibility visibility;
    private boolean isStatic;

    public String getName();
    public String getType();
    public Visibility getVisibility();
}
```

### `Method.java`
```java
public class Method {
    private int methodId;
    private String name;
    private String returnType;
    private List<String> parameters;
    private Visibility visibility;
    private boolean isAbstract;

    public String getSignature();
    public String getName();
    public String getReturnType();
}
```

### `Relationship.java` (abstract base)
```java
public abstract class Relationship {
    private int relationshipId;
    private UMLClass sourceClass;
    private UMLClass targetClass;
    private String sourceMultiplicity;
    private String targetMultiplicity;
    private String label;

    public abstract RelationshipType getType();
    public UMLClass getSource();
    public UMLClass getTarget();
    public String getSourceMultiplicity();
    public String getTargetMultiplicity();
    public boolean validate();
}
```

### `InheritanceRelationship.java`
```java
public class InheritanceRelationship extends Relationship {
    private boolean isInterface;

    @Override
    public RelationshipType getType() { return RelationshipType.INHERITANCE; }
}
```

### `AssociationRelationship.java`
```java
public class AssociationRelationship extends Relationship {
    private boolean isComposition;
    private boolean isAggregation;
    private Navigability navigability;

    @Override
    public RelationshipType getType();
    public boolean isComposition();
    public boolean isAggregation();
}
```

### `DependencyRelationship.java`
```java
public class DependencyRelationship extends Relationship {
    private String dependencyType;

    @Override
    public RelationshipType getType() { return RelationshipType.DEPENDENCY; }
}
```

### `ChatMessage.java`
```java
public class ChatMessage {
    private int messageId;
    private String content;
    private SenderType sender;
    private Date timestamp;
    private int projectId;

    public String getContent();
    public SenderType getSender();
    public Date getTimestamp();
}
```

### `EvaluationReport.java`
```java
public class EvaluationReport {
    private int reportId;
    private int diagramId;
    private int projectId;
    private double couplingScore;
    private double cohesionScore;
    private double solidScore;
    private List<String> suggestions;
    private Date generatedDate;

    public String getSummary();
    public List<String> getSuggestions();
    public double getOverallScore();
}
```

### `StructureSuggestion.java`
```java
public class StructureSuggestion {
    private int suggestionId;
    private int diagramId;
    private Map<String, String> codeSkeletons;
    private Date generatedDate;

    public Map<String, String> getSkeletons();
    public void exportCode(String path);
}
```

### `UMLModel.java`
```java
public class UMLModel {
    private List<UMLClass> classes;
    private List<Relationship> relationships;
    private String rawJson;

    public UMLDiagram toUMLDiagram();
    public boolean validate();
}
```

### `ProjectContext.java`
```java
public class ProjectContext {
    private Project project;
    private UMLDiagram currentDiagram;
    private List<ChatMessage> chatHistory;

    public String buildContextPrompt();
}
```

### `DiagramEdit.java`
```java
public class DiagramEdit {
    private EditType editType;
    private int targetClassId;
    private Map<String, Object> payload;

    public void apply(UMLDiagram diagram);
}
```

---

## 6. Controllers (Section `controllers/`)

### `ProjectController.java`
```java
// GRASP: Controller (Facade / Boundary)
public class ProjectController {
    private IProjectRepository projectRepo;
    private IDiagramRepository diagramRepo;

    public ProjectController(IProjectRepository projectRepo, IDiagramRepository diagramRepo);

    public Project createProject(String name, String desc);
    public Project retrieveProject(int id);
    public void maintainProject(int id, String name, String desc);
    public void deleteProject(int id);
    public List<Project> listAllProjects();
}
```

**Implementation Notes (UC1, UC9, UC10 aligned):**
- `createProject`: validate non-empty name, unique name across all projects, then persist. Throw `ValidationException` on duplicate or empty name (maps to SSD1 step 5 alt/else).
- `retrieveProject`: fetch project + all its diagrams + chat history + evaluations from DB (SSD10 step 4).
- `maintainProject`: validate new name for uniqueness before update. On DB failure, throw `DatabaseException` and rollback (UC9 extension 6a).
- `listAllProjects`: if empty, return empty list (UC10 extension 2a).

### `DiagramController.java`
```java
// GRASP: Controller (Facade / Boundary)
public class DiagramController {
    private IDiagramRepository diagramRepo;
    private IAIEngine aiEngine;
    private ICodeParser codeParser;
    private IExportService exportSvc;

    public DiagramController(IDiagramRepository diagramRepo, IAIEngine aiEngine,
                             ICodeParser codeParser, IExportService exportSvc);

    public UMLDiagram generateFromText(String description, int projectId);
    public UMLDiagram generateFromCode(List<File> sourceFiles, int projectId);
    public void applyEdit(DiagramEdit edit, UMLDiagram diagram);
    public void saveDiagram(UMLDiagram diagram);
    public void exportDiagram(UMLDiagram diagram, ExportFormat fmt, String path);
    public UMLDiagram analyzeDiagramImage(byte[] imageData, int projectId);
}
```

**Implementation Notes (UC2, UC3, UC4, UC8, UC12 aligned):**
- `generateFromText`: validate description for minimum length (>10 chars, minimum coherence check). Call `aiEngine.generateFromText()`. Parse returned `UMLModel` into `UMLDiagram`. Map SSD2 flow exactly: steps 3→6→8→10.
- `generateFromCode`: validate file formats (accept `.java` only per scope). Delegate to `codeParser.parse()`. Maps SSD3 flow exactly.
- `applyEdit`: apply `DiagramEdit.apply()` to the diagram, then validate structural integrity (no orphan relationships, no circular dependencies that aren't allowed — warn but don't block). Maps SSD4.
- `exportDiagram`: validate format support first. On incompatible format, throw `ExportException` which triggers the alt branch in SSD12 / UC12 extension 4a.
- `analyzeDiagramImage`: validate image format/size before calling AI. Maps SSD8.

### `AIController.java`
```java
// GRASP: Controller (Facade / Boundary)
public class AIController {
    private IAIEngine aiEngine;
    private IChatRepository chatRepo;
    private IEvaluationRepository evalRepo;

    public AIController(IAIEngine aiEngine, IChatRepository chatRepo,
                        IEvaluationRepository evalRepo);

    public EvaluationReport evaluateDesign(int diagramId);
    public ChatMessage consultAI(String query, int projectId);
    public StructureSuggestion generateStructureSuggestions(int diagramId);
    public List<ChatMessage> getChatHistory(int projectId);
}
```

**Implementation Notes (UC5, UC6, UC7 aligned):**
- `evaluateDesign`: serialize the diagram to JSON, build evaluation prompt (include coupling, cohesion, SOLID criteria explicitly in the prompt). Persist result via `evalRepo.save()`. Maps SSD5.
- `consultAI`: load `ProjectContext` (current diagram + chat history), call `aiEngine.consultDesign()`, persist both the user message and AI response via `chatRepo.save()`. Maps SSD6 loop.
- `generateStructureSuggestions`: extract full `UMLModel` from diagram, call `aiEngine.generateStructure()`, parse response into `StructureSuggestion`, persist. Maps SSD7.

---

## 7. Repository Implementations (Section `repository/`)

### `DatabaseManager.java` (Singleton)
```java
// GRASP: Pure Fabrication
public class DatabaseManager {
    private static DatabaseManager instance;
    private ConnectionPool connectionPool;
    private String jdbcUrl;
    private String username;

    public static DatabaseManager getInstance();
    public Connection getConnection();
    public void closeConnection(Connection c);
    public ResultSet executeQuery(String sql);
    public int executeUpdate(String sql);
}
```

**Critical:** Implement as a singleton with connection pooling. Load `jdbcUrl` from `config.properties`. Support both SQLite (default/dev) and PostgreSQL (production). Wrap all mutations in transactions; on failure call `connection.rollback()`.

### Database Schema (`schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS projects (
    project_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_date TEXT NOT NULL,
    last_modified_date TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS uml_diagrams (
    diagram_id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    source_type TEXT NOT NULL CHECK(source_type IN ('NATURAL_LANGUAGE','SOURCE_CODE','UPLOADED')),
    created_date TEXT NOT NULL,
    last_modified_date TEXT NOT NULL,
    serialized_model TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS uml_classes (
    class_id INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    is_abstract INTEGER DEFAULT 0,
    is_interface INTEGER DEFAULT 0,
    position_x REAL DEFAULT 0,
    position_y REAL DEFAULT 0,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attributes (
    attribute_id INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    visibility TEXT NOT NULL CHECK(visibility IN ('PUBLIC','PRIVATE','PROTECTED')),
    is_static INTEGER DEFAULT 0,
    FOREIGN KEY (class_id) REFERENCES uml_classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS methods (
    method_id INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    return_type TEXT NOT NULL,
    parameters TEXT,
    visibility TEXT NOT NULL CHECK(visibility IN ('PUBLIC','PRIVATE','PROTECTED')),
    is_abstract INTEGER DEFAULT 0,
    FOREIGN KEY (class_id) REFERENCES uml_classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS relationships (
    relationship_id INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id INTEGER NOT NULL,
    source_class_id INTEGER NOT NULL,
    target_class_id INTEGER NOT NULL,
    relationship_type TEXT NOT NULL CHECK(relationship_type IN ('ASSOCIATION','INHERITANCE','DEPENDENCY','COMPOSITION','AGGREGATION')),
    source_multiplicity TEXT,
    target_multiplicity TEXT,
    label TEXT,
    is_composition INTEGER DEFAULT 0,
    is_aggregation INTEGER DEFAULT 0,
    is_interface INTEGER DEFAULT 0,
    navigability TEXT DEFAULT 'BOTH',
    dependency_type TEXT,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    sender TEXT NOT NULL CHECK(sender IN ('USER','AI','SYSTEM')),
    timestamp TEXT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS evaluation_reports (
    report_id INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    coupling_score REAL,
    cohesion_score REAL,
    solid_score REAL,
    suggestions TEXT,
    generated_date TEXT NOT NULL,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS structure_suggestions (
    suggestion_id INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id INTEGER NOT NULL,
    code_skeletons TEXT NOT NULL,
    generated_date TEXT NOT NULL,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);
```

### Repository Implementations

Each repository (`ProjectRepositoryImpl`, `DiagramRepositoryImpl`, `ChatRepositoryImpl`, `EvaluationRepositoryImpl`) must:
- Implement the corresponding interface exactly.
- Use `DatabaseManager.getInstance().getConnection()` for every operation.
- Use `PreparedStatement` for all queries (no string concatenation — SQL injection prevention).
- Wrap all writes in try-with-resources with explicit rollback in catch blocks.
- Map all ResultSet columns to domain object fields with type safety.
- Follow **GRASP Pure Fabrication** pattern (note this as a comment in each class).

---

## 8. Service Implementations (Section `services/`)

### `LLMAPIEngine.java`
```java
// GRASP: Pure Fabrication
public class LLMAPIEngine implements IAIEngine {
    private String apiKey;
    private String apiEndpoint;
    private String modelName;
    private HttpClient httpClient;

    // Implements all IAIEngine methods
    public UMLModel generateFromText(String desc);
    public EvaluationReport evaluateDesign(UMLModel m);
    public String consultDesign(String q, ProjectContext ctx);
    public String generateStructure(UMLModel m);
    public UMLModel analyzeImage(byte[] data);

    private String buildPrompt(String input);
    private UMLModel parseResponse(String json);
}
```

**LLM Integration Details:**
- Load `apiKey`, `apiEndpoint`, `modelName` from `config.properties` (never hardcode).
- Use OkHttp for all HTTP calls with a 30-second timeout.
- `generateFromText`: system prompt instructs the AI to return JSON matching UMLModel schema: `{"classes":[{"name":"","isAbstract":false,"isInterface":false,"attributes":[{"name":"","type":"","visibility":"PRIVATE"}],"methods":[{"name":"","returnType":"","parameters":[],"visibility":"PUBLIC"}]}],"relationships":[{"sourceClass":"","targetClass":"","type":"ASSOCIATION","sourceMultiplicity":"1","targetMultiplicity":"*"}]}`. Parse this JSON into `UMLModel`.
- `evaluateDesign`: serialize `UMLModel` to JSON, attach to prompt asking for coupling/cohesion/SOLID analysis, return structured JSON: `{"couplingScore":0.0,"cohesionScore":0.0,"solidScore":0.0,"suggestions":[],"summary":""}`.
- `consultDesign`: prepend `ProjectContext.buildContextPrompt()` as the system context. Append conversation history (last 10 messages).
- `generateStructure`: ask for Java class skeleton code per class in the model. Return as `Map<className, codeString>`.
- `analyzeImage`: convert `byte[]` to Base64, send as multipart if the API supports vision. Return `UMLModel` parsed from the AI's interpretation.
- On `IOException` or non-200 response: throw custom `AIEngineException`. All controllers must catch this and propagate to the UI as user-friendly error dialogs.

### `JavaCodeParser.java`
```java
// GRASP: Pure Fabrication
public class JavaCodeParser implements ICodeParser {
    private JavaParserLib parserLib;          // com.github.javaparser
    private List<String> supportedLang;

    public UMLModel parse(List<File> files);
    public List<String> getSupportedLanguages();
    private List<UMLClass> extractClasses(AST ast);
    private List<Relationship> extractRelationships(AST ast);
}
```

**Parser Implementation:**
- Use `com.github.javaparser.StaticJavaParser` to parse each file.
- `extractClasses`: visit `ClassOrInterfaceDeclaration` nodes. Extract name, isAbstract, isInterface, then fields → `Attribute` objects, methods → `Method` objects.
- `extractRelationships`: detect `extends` → `InheritanceRelationship`; field types matching other parsed classes → `AssociationRelationship`; `implements` → `InheritanceRelationship` with `isInterface=true`.
- Aggregate all files into one `UMLModel`. Deduplicate relationships.
- On `ParseProblemException`: throw `ParsingException` with the failing file name and line number (maps to UC3 extension 6a).

### `DiagramExportService.java`
```java
// GRASP: Pure Fabrication
public class DiagramExportService implements IExportService {
    private RenderEngine renderEngine;

    public void export(UMLDiagram d, ExportFormat fmt, String path);
    public List<ExportFormat> getSupportedFormats();
    private byte[] renderToPNG(UMLDiagram d);
    private byte[] renderToPDF(UMLDiagram d);
    private byte[] renderToSVG(UMLDiagram d);
}
```

**Export Implementation:**
- `renderToPNG`: snapshot the `DiagramCanvas` JavaFX node using `WritableImage`, then encode as PNG with `ImageIO`.
- `renderToPDF`: render canvas to `BufferedImage`, embed in PDFBox `PDDocument`.
- `renderToSVG`: use Apache Batik `SVGGraphics2D` to re-draw the diagram in SVG format by iterating `UMLClass` and `Relationship` objects.
- Validate `fmt` against `getSupportedFormats()` before rendering. Throw `UnsupportedFormatException` (maps to UC12 extension 4a).

### `SpeechToTextServiceImpl.java`
```java
// GRASP: Pure Fabrication, Low Coupling
public class SpeechToTextServiceImpl implements ISpeechToTextService {
    private AudioCapture audioCapture;
    private STTAPIClient sttAPI;

    public void startRecording();
    public String stopRecording();
    public boolean isAvailable();
}
```

**Voice Implementation:**
- Use `javax.sound.sampled.TargetDataLine` for `AudioCapture`.
- `isAvailable()`: check microphone presence via `AudioSystem.getMixerInfo()`. Return false if none available.
- `startRecording()`: open `TargetDataLine`, start capturing 16kHz mono PCM audio on a background thread.
- `stopRecording()`: close the line, send WAV bytes to a speech-to-text REST API (Whisper, Google STT, or local Vosk — configurable via `config.properties`). Return transcribed text string.
- If API unavailable: throw `SpeechServiceException` (maps to UC11 extension 6b).
- If no microphone: throw `HardwareException` (maps to UC11 extension 4a).

---

## 9. UI Layer — JavaFX (Section `ui/`)

### `MainWindow.java`
```java
public class MainWindow extends Application {
    private ProjectController projectCtrl;
    private DiagramController diagramCtrl;
    private AIController aiCtrl;
    private Project activeProject;

    public void openProject();
    public void createProject();
    public void showDiagramEditor();
    public void showChatPanel();
    public void showEvaluationPanel();
    public void initialize();

    @Override
    public void start(Stage primaryStage);
}
```

**MainWindow Layout:**
- Top: `MenuBar` with menus: **File** (New Project, Open Project, Exit), **Diagram** (Generate from Text, Generate from Code, Upload Image, Export), **AI** (Evaluate Design, Consult AI, Generate Suggestions), **Help**.
- Below MenuBar: `ToolBar` with icon buttons for common actions.
- Center: `TabPane` where each tab corresponds to an open project/diagram. Default tab shows `ProjectDashboardPanel`.
- Bottom: `StatusBar` showing connection status, AI availability indicator, and last-saved timestamp.
- The app title must be "UMLytics — AI-Powered UML Design Intelligence".
- On startup: call `DatabaseManager.getInstance()` to initialize the DB, run `schema.sql` if tables don't exist, then show `ProjectDashboardPanel`.

### `ProjectDashboardPanel.java`
```java
public class ProjectDashboardPanel extends VBox {
    private ProjectController projectCtrl;
    private ListView<Project> projectList;     // JavaFX ListView (replaces JList)

    public void onCreateProject();
    public void onOpenProject();
    public void displayProjects(List<Project> list);
}
```

**Dashboard Layout:**
- Left sidebar: `ListView<Project>` showing all saved projects with name, diagram count, and last modified date.
- Right panel: project details form showing name, description, creation date, diagrams. Buttons: "Open", "Edit", "Delete".
- "New Project" button opens a modal `Dialog` with `TextField` for name and `TextArea` for description.
- On "Open": load full project via `projectCtrl.retrieveProject()`, then open `DiagramEditorPanel` in a new tab.
- On "Edit": inline edit form that calls `projectCtrl.maintainProject()`. Show success/error `Alert`.
- If no projects exist, show a centered placeholder: "No projects yet. Create one to get started." with a large "New Project" button.

### `EvaluationPanel.java`
```java
public class EvaluationPanel extends VBox {
    private AIController aiCtrl;

    public void onEvaluate();
    public void displayReport(EvaluationReport r);
}
```

**Evaluation Layout:**
- Top: "Evaluate Design" button.
- Results area: three circular progress indicators for Coupling Score, Cohesion Score, and SOLID Score (0–10 scale), color-coded (green ≥ 7, yellow 4–6, red < 4).
- Below indicators: scrollable `ListView<String>` of suggestions, each shown as a card with an icon (warning/info/error).
- Export button to save the report as a PDF summary.
- While evaluation is running: disable the button and show a `ProgressIndicator` spinner.

### `ChatPanel.java`
```java
public class ChatPanel extends VBox {
    private AIController aiCtrl;
    private ISpeechToTextService speechSvc;
    private TextArea inputField;              // JavaFX TextArea
    private ListView<ChatMessage> chatDisplay; // JavaFX ListView

    public void onSendMessage();
    public void onVoiceInput();
    public void displayMessages(List<ChatMessage> msgs);
}
```

**Chat Layout:**
- Exact replica of a modern chat UI (think ChatGPT/Claude style):
  - Scrollable messages area. USER messages right-aligned, dark background. AI messages left-aligned, light background. Each message shows sender label and timestamp.
  - Bottom input bar: `TextArea` (Shift+Enter for newline, Enter to send), microphone icon button (calls `onVoiceInput()`), and send button.
  - Voice input: shows a pulsing red recording indicator animation while capturing.
  - Markdown-like formatting in AI responses: bold (`**text**`), code blocks (`\`code\``), bullet lists.
  - On send: immediately append user message to list, show a typing indicator (animated dots) while waiting for AI, then replace with AI response.

---

## 10. Diagram Editor — draw.io Exact Replica (`ui/canvas/`)

This is the most critical section. `DiagramEditorPanel` must be a **pixel-faithful recreation of draw.io**. Every UI element described below is mandatory.

### Overall Layout (exactly draw.io)

```
┌─────────────────────────────────────────────────────────────────┐
│  [File][Edit][View][Extras][Help]   [Fit][Zoom-][100%][Zoom+]  │  ← Menu + Zoom toolbar
├──────────┬──────────────────────────────────────┬───────────────┤
│          │  [Select][Pan][Move][Text][Connect]  │               │  ← Edit toolbar (top of canvas)
│  SHAPES  │────────────────────────────────────  │   FORMAT      │
│  PANEL   │                                      │   PANEL       │
│  (left)  │         CANVAS                       │   (right)     │
│          │     (infinite, scrollable,           │               │
│  Search  │      grid, pan, zoom)                │  Style        │
│ ──────── │                                      │  Arrange      │
│ General  │                                      │  Connection   │
│  □ Class │                                      │               │
│  □ Note  │                                      │               │
│  □ Iface │                                      │               │
│ ──────── │                                      │               │
│ UML      │                                      │               │
│  □ Class │                                      │               │
│  □ Enum  │                                      │               │
└──────────┴──────────────────────────────────────┴───────────────┘
│  Page: [+][1][2]...              zoom: 100%   cursor: 0,0      │  ← Status bar
└─────────────────────────────────────────────────────────────────┘
```

### `DiagramEditorPanel.java`
```java
public class DiagramEditorPanel extends BorderPane {
    private DiagramCanvas canvas;
    private DiagramController diagramCtrl;
    private EditToolBar toolBar;

    public void onAddClass();
    public void onAddRelationship();
    public void onSaveDiagram();
    public void onExport();
    public void renderDiagram(UMLDiagram d);
}
```

### `DiagramCanvas.java` — The Core Canvas

Implement as a JavaFX `Canvas` inside a `ScrollPane` (or use a `Pane` with clipping for infinite scroll).

**Grid:**
- Draw a background dot-grid (like draw.io) at 10px intervals. Grid lines: `#e0e0e0`.
- Grid snapping: all class nodes snap to a 10px grid.

**Pan and Zoom:**
- Mouse drag on empty canvas (or Space+drag): pan the viewport using `canvas.setTranslateX/Y`.
- Mouse scroll wheel: zoom in/out, keeping the cursor position as the zoom center. Zoom range: 10% to 400%.
- Pinch gesture (trackpad): zoom.
- `Ctrl+Shift+H`: fit diagram to window.
- Zoom display in the status bar must update in real time.

**Class Node rendering (`ClassNode.java`):**

Each `UMLClass` is rendered as a three-section rectangle matching draw.io's UML class style exactly:

```
┌─────────────────────────────┐
│  <<interface>>               │  ← stereotype (if interface or abstract)
│  ClassName                   │  ← class name — bold, centered
├─────────────────────────────┤
│  - attributeName: Type       │  ← attributes, left-aligned
│  # staticField: Type         │     prefix: - private, + public, # protected
├─────────────────────────────┤
│  + methodName(): ReturnType  │  ← methods, left-aligned
│  + otherMethod(p: T): void   │
└─────────────────────────────┘
```

- Header section: background `#dae8fc` for regular classes, `#d5e8d4` for interfaces, `#ffe6cc` for abstract classes.
- Border: 1px solid `#6c8ebf`.
- Font: class name `14px bold`, members `12px regular`.
- Minimum width: 200px. Auto-expand height based on member count.
- Selected state: 4 blue resize handles at corners + 4 at midpoints (exactly like draw.io selection handles). Selection highlight: `2px solid #0066ff`.
- Hover state: faint blue outline on hover.

**Class Node Interactions:**
- **Double-click on class name:** inline-edit the class name (show a `TextField` overlay at that position).
- **Double-click on attribute/method:** inline-edit that line.
- **Drag class node:** move it on the canvas with grid snapping. All connected edges must follow (rubber-band routing).
- **Resize handles:** drag corner/edge handles to resize the class box.
- **Right-click context menu:** "Add Attribute", "Add Method", "Edit Class", "Delete Class", "Add Relationship →", "Set Abstract", "Set Interface", "Copy", "Paste".
- **Ctrl+click:** multi-select nodes.
- **Click empty canvas:** deselect all.

**Relationship/Edge rendering (`RelationshipEdge.java`):**

Render using orthogonal routing (like draw.io default — edges run horizontally/vertically with 90° bends). Support:

| RelationshipType | Line Style | Arrow/End marker |
|---|---|---|
| ASSOCIATION | Solid | Open arrowhead → |
| INHERITANCE (class) | Solid | Hollow triangle → (extends) |
| INHERITANCE (interface) | Dashed | Hollow triangle → (implements) |
| COMPOSITION | Solid | Filled diamond ◆ + open arrow |
| AGGREGATION | Solid | Hollow diamond ◇ + open arrow |
| DEPENDENCY | Dashed | Open arrowhead → |

- Multiplicity labels float near the source/target ends of the edge.
- Edge label (middle): editable by double-clicking.
- Edge selected: show blue diamond waypoint handles at bend points. Drag to reroute.
- Hover: highlight edge in blue.

**Drawing Relationships:**
- Hover over a class node: show 4 blue directional arrows (N/S/E/W) on the edges — like draw.io's connection points.
- Click and drag from a connection arrow to another class: creates a new relationship.
- On drop: show a popup menu: "Association", "Inheritance", "Composition", "Aggregation", "Dependency".

**`EditToolBar.java`:**

Draw.io-style floating toolbar at the top of the canvas area:

```
[↖ Select] [✥ Pan] [⬜ Class] [🔗 Relationship] [T Text] | [↩ Undo] [↪ Redo] | [📋 Format]
```

- Select mode: default. Click/drag to select nodes or create selection box.
- Pan mode: click-drag to scroll canvas (alternative to Space).
- Class tool: click anywhere on canvas → drops a new `UMLClass` node at that position with a default name "NewClass" (user can immediately rename).
- Relationship tool: click source class → click target class → type dialog.
- Undo/Redo stack: maintain a `Deque<DiagramEdit>` with unlimited undo using `DiagramEdit.apply()`.

**Left Shape Panel:**
Collapsible left sidebar (toggle button, 240px wide). Sections:
- **Search shapes** text field at top.
- **UML** section: drag-able shape templates for Class, Interface, Abstract Class, Enumeration, Note/Comment.
- Drag from panel → drop onto canvas to add the shape.

**Right Format Panel:**
Collapsible right sidebar (300px wide). Context-sensitive:
- **No selection:** shows "Style" tab with canvas properties (grid toggle, background color).
- **Class selected:** shows:
  - Style tab: fill color picker, border color, font size selector.
  - Arrange tab: X/Y position fields, Width/Height fields, "To Front"/"To Back" buttons.
- **Edge selected:** shows:
  - Connection tab: source/target multiplicity text fields, label field, line style (solid/dashed), arrow start/end selectors matching all `RelationshipType` options.

**Mini-map:**
Bottom-right corner: a small thumbnail of the full diagram (100×80px) showing the current viewport rectangle. Click to jump to position.

**Page Tabs:**
Bottom bar: page tab strip (draw.io style) with "+" button to add pages. Each UMLDiagram is one page. Show page name, allow double-click rename.

---

## 11. SSD Flow Implementation Checklist

Every SSD must be traceable to code. Verify each:

| SSD | Key Interactions | Code Location |
|-----|-----------------|---------------|
| SSD1 | selectNewProject → validation → initWorkspace / validationError | `ProjectController.createProject()` + `ProjectDashboardPanel.onCreateProject()` |
| SSD2 | submitDescription → AIEngine.generateUML → render diagram | `DiagramController.generateFromText()` + `DiagramEditorPanel.renderDiagram()` |
| SSD3 | uploadFiles → codeParser.parse → renderDiagram | `DiagramController.generateFromCode()` + file chooser dialog |
| SSD4 | modifyClass → applyEdit → updateCanvas → saveRefinedDesign | `DiagramCanvas` interactions + `DiagramController.applyEdit()` |
| SSD5 | requestEvaluation → AIEngine.evaluateDesign → categorizedReport | `AIController.evaluateDesign()` + `EvaluationPanel.displayReport()` |
| SSD6 | loop: submitQuestion → AIEngine.consultDesign → chatResponse | `AIController.consultAI()` + `ChatPanel` message loop |
| SSD7 | requestStructureSuggestions → AIEngine.generateStructure → suggestions | `AIController.generateStructureSuggestions()` |
| SSD8 | uploadUMLImage → AIEngine.analyzeImage → extractedElements | `DiagramController.analyzeDiagramImage()` + import offer dialog |
| SSD9 | openSettings → updateMetadata → DB.store → confirmation | `ProjectController.maintainProject()` + settings dialog |
| SSD10 | openProjectMenu → DB.findAll → selectProject → loadWorkspace | `ProjectController.listAllProjects()` + `ProjectDashboardPanel` |
| SSD11 | activateVoice → recordAudio → STT.convert → populateField | `SpeechToTextServiceImpl` + `ChatPanel.onVoiceInput()` |
| SSD12 | requestExport → configureOptions → generateFile / formatWarning | `DiagramExportService.export()` + export dialog with alt branch |

---

## 12. Error Handling & Extension Scenarios

Implement every extension from the use cases. Each must produce a visible UI response:

| Scenario | Exception Class | UI Response |
|----------|----------------|-------------|
| UC1 3a: duplicate/empty name | `ValidationException` | Red inline error text under the name field |
| UC1 6a: DB failure during create | `DatabaseException` | Error `Alert` dialog + rollback |
| UC2 4a: description too short | `ValidationException` | Yellow warning label, disable Generate button |
| UC2 6a: AI service unavailable | `AIEngineException` | Error dialog with "Retry" button |
| UC2 8a: AI returns invalid response | `ParseResponseException` | Error dialog offering to retry with refined prompt |
| UC3 4a: unsupported file format | `UnsupportedFileException` | Inline error in file chooser |
| UC3 6a: parse syntax error | `ParsingException` | Dialog showing file name and line number |
| UC4 6a: circular dependency | `DiagramValidationWarning` | Yellow warning banner (non-blocking) |
| UC4 8a: DB save fail | `DatabaseException` | Error dialog, retain in-memory state |
| UC5 2a: diagram too simple | `DiagramTooSimpleException` | Info dialog: "At least 2 classes required" |
| UC7 2a: no classes defined | `EmptyDiagramException` | Info dialog |
| UC9 4a: name conflict | `ValidationException` | Inline error on name field |
| UC10 2a: no projects exist | None | Empty state UI with "Create New Project" |
| UC11 4a: no microphone | `HardwareException` | Error dialog with instructions |
| UC11 6b: STT unavailable | `SpeechServiceException` | Info: "Voice unavailable. Use text input." |
| UC12 4a: incompatible format | `UnsupportedFormatException` | Warning dialog with alternative format suggestions |

---

## 13. AI Prompt Templates

Store prompt templates in `config.properties` or a dedicated `prompts/` resource directory. These are the exact prompts to use:

**UML Generation from Text:**
```
System: You are a UML class diagram expert. Given a natural language description, generate a UML class diagram as structured JSON. Return ONLY valid JSON, no markdown, no explanation.
Schema: {"classes":[{"name":"String","isAbstract":bool,"isInterface":bool,"attributes":[{"name":"String","type":"String","visibility":"PUBLIC|PRIVATE|PROTECTED","isStatic":bool}],"methods":[{"name":"String","returnType":"String","parameters":["String"],"visibility":"PUBLIC|PRIVATE|PROTECTED","isAbstract":bool}]}],"relationships":[{"sourceClass":"String","targetClass":"String","type":"ASSOCIATION|INHERITANCE|COMPOSITION|AGGREGATION|DEPENDENCY","sourceMultiplicity":"String","targetMultiplicity":"String","label":"String"}]}
User: {userDescription}
```

**Design Evaluation:**
```
System: You are a Software Design and Architecture expert. Evaluate the provided UML class diagram JSON against these principles: Coupling (low is better), Cohesion (high is better), and SOLID (SRP, OCP, LSP, ISP, DIP). Return ONLY valid JSON.
Schema: {"couplingScore":0.0-10.0,"cohesionScore":0.0-10.0,"solidScore":0.0-10.0,"violations":[{"principle":"String","className":"String","description":"String","severity":"ERROR|WARNING|INFO"}],"summary":"String"}
Diagram: {serializedDiagramJson}
```

**AI Design Consultation:**
```
System: You are a software design mentor specialized in UML and OOP architecture. You have context about the current project. Be concise, educational, and reference SDA principles.
Context: {projectContextPrompt}
Chat History: {last10MessagesJson}
User: {userQuestion}
```

**Structure Suggestion:**
```
System: Generate Java class skeleton code for each class in the provided UML model. For each class, produce compilable Java code with field declarations, constructors, and method stubs. Return ONLY valid JSON.
Schema: {"skeletons":{"ClassName":"javaCodeString"}}
Model: {umlModelJson}
```

---

## 14. Configuration (`config.properties`)

```properties
# Database
db.url=jdbc:sqlite:umlytics.db
db.username=
db.password=

# LLM API
ai.api.key=YOUR_API_KEY_HERE
ai.api.endpoint=https://api.openai.com/v1/chat/completions
ai.api.model=gpt-4o

# Speech to Text
stt.provider=whisper
stt.api.key=YOUR_STT_KEY_HERE
stt.api.endpoint=https://api.openai.com/v1/audio/transcriptions

# Export
export.default.path=./exports

# UI
ui.theme=light
ui.canvas.grid.size=10
ui.canvas.default.zoom=1.0
```

---

## 15. Application Entry Point

```java
// Main.java
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Initialize database
        DatabaseManager.getInstance().initialize();

        // 2. Wire dependencies (manual DI)
        IProjectRepository projectRepo = new ProjectRepositoryImpl(DatabaseManager.getInstance().getConnection());
        IDiagramRepository diagramRepo = new DiagramRepositoryImpl(DatabaseManager.getInstance().getConnection());
        IChatRepository chatRepo = new ChatRepositoryImpl(DatabaseManager.getInstance().getConnection());
        IEvaluationRepository evalRepo = new EvaluationRepositoryImpl(DatabaseManager.getInstance().getConnection());
        IAIEngine aiEngine = new LLMAPIEngine(/* load from config */);
        ICodeParser codeParser = new JavaCodeParser();
        IExportService exportSvc = new DiagramExportService();
        ISpeechToTextService sttSvc = new SpeechToTextServiceImpl();

        ProjectController projectCtrl = new ProjectController(projectRepo, diagramRepo);
        DiagramController diagramCtrl = new DiagramController(diagramRepo, aiEngine, codeParser, exportSvc);
        AIController aiCtrl = new AIController(aiEngine, chatRepo, evalRepo);

        // 3. Launch MainWindow
        MainWindow mainWindow = new MainWindow(projectCtrl, diagramCtrl, aiCtrl);
        mainWindow.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 16. CSS Styling (`drawio-theme.css`)

The application must look like draw.io. Apply this CSS via JavaFX's `scene.getStylesheets().add()`:

```css
/* Draw.io color palette */
.root {
    -fx-base: #ffffff;
    -fx-background: #f5f5f5;
    -fx-control-inner-background: #ffffff;
}

/* Toolbar */
.tool-bar {
    -fx-background-color: #f5f5f5;
    -fx-border-color: #d0d0d0;
    -fx-border-width: 0 0 1 0;
    -fx-padding: 4px 8px;
}

/* Canvas background */
.diagram-canvas-container {
    -fx-background-color: #ffffff;
}

/* Left shape panel */
.shape-panel {
    -fx-background-color: #f5f5f5;
    -fx-border-color: #d0d0d0;
    -fx-border-width: 0 1 0 0;
    -fx-min-width: 240px;
    -fx-max-width: 240px;
}

/* Right format panel */
.format-panel {
    -fx-background-color: #f5f5f5;
    -fx-border-color: #d0d0d0;
    -fx-border-width: 0 0 0 1;
    -fx-min-width: 260px;
    -fx-max-width: 260px;
}

/* Class node */
.class-node-header {
    -fx-background-color: #dae8fc;
    -fx-border-color: #6c8ebf;
    -fx-font-weight: bold;
}

.class-node-interface-header {
    -fx-background-color: #d5e8d4;
    -fx-border-color: #82b366;
}

.class-node-abstract-header {
    -fx-background-color: #ffe6cc;
    -fx-border-color: #d6b656;
}

/* Selected state */
.class-node-selected {
    -fx-border-color: #0066ff;
    -fx-border-width: 2px;
    -fx-effect: dropshadow(three-pass-box, #0066ff, 4, 0, 0, 0);
}

/* Chat bubbles */
.chat-bubble-user {
    -fx-background-color: #1a73e8;
    -fx-text-fill: white;
    -fx-background-radius: 12px 12px 0 12px;
    -fx-padding: 8px 12px;
}

.chat-bubble-ai {
    -fx-background-color: #f0f4f9;
    -fx-text-fill: #202124;
    -fx-background-radius: 12px 12px 12px 0;
    -fx-padding: 8px 12px;
}

/* Evaluation score circles */
.score-good { -fx-stroke: #34a853; }
.score-medium { -fx-stroke: #fbbc04; }
.score-poor { -fx-stroke: #ea4335; }
```

---

## 17. Keyboard Shortcuts (draw.io standard)

| Shortcut | Action |
|----------|--------|
| Ctrl+N | New Project |
| Ctrl+O | Open Project |
| Ctrl+S | Save Diagram |
| Ctrl+Z | Undo |
| Ctrl+Y / Ctrl+Shift+Z | Redo |
| Ctrl+A | Select All |
| Delete / Backspace | Delete selected |
| Ctrl+C | Copy |
| Ctrl+V | Paste |
| Ctrl++ / Ctrl+= | Zoom In |
| Ctrl+- | Zoom Out |
| Ctrl+Shift+H | Fit to window |
| Ctrl+Shift+F | Format panel toggle |
| Escape | Cancel current tool / deselect |
| Space | Pan mode (hold) |
| F2 | Edit selected node name |
| Ctrl+E | Export diagram |

---

## 18. Testing Requirements

Write JUnit 5 tests for:

1. **`ProjectControllerTest`:** test `createProject` with valid input, duplicate name, empty name; test `maintainProject` with name conflict; test `listAllProjects` with empty DB.
2. **`DiagramControllerTest`:** test `generateFromText` with valid/invalid/too-short descriptions; test `exportDiagram` with each `ExportFormat`; test `generateFromCode` with valid `.java` file and invalid file type.
3. **`AIControllerTest`:** mock `IAIEngine`; test `evaluateDesign` with diagram having < 2 classes; test `consultAI` stores both messages.
4. **`LLMAPIEngineTest`:** mock HTTP client; test `parseResponse` with valid JSON, malformed JSON, empty response.
5. **`JavaCodeParserTest`:** test with a sample `.java` file containing classes, inheritance, and fields.
6. **`DatabaseManagerTest`:** test connection, executeQuery, executeUpdate, rollback.
7. **`DiagramExportServiceTest`:** test each format export produces non-empty bytes.

---

## 19. Final Consistency Checklist

Before considering the implementation complete, verify:

- [ ] Every class in the class diagram exists with exact field names and types
- [ ] Every interface method signature matches the diagram exactly
- [ ] All 7 enums are implemented with all values from the diagram
- [ ] All 12 Use Cases are implemented end-to-end
- [ ] All 12 SSD flows are traceable to code
- [ ] All extension scenarios show appropriate UI feedback
- [ ] The diagram editor looks and behaves like draw.io
- [ ] All relationship types render with correct arrow styles
- [ ] Inline editing works on class names, attributes, and methods
- [ ] Undo/Redo works for all edit operations
- [ ] Export to PNG, PDF, SVG all produce correct output
- [ ] Voice input activates recording, shows indicator, populates field
- [ ] AI consultation persists chat history in DB per project
- [ ] Evaluation report stores in DB and shows scoring UI
- [ ] Structure suggestions display with syntax-highlighted code viewer
- [ ] Image upload → AI analysis → import offer flow works end-to-end
- [ ] Project CRUD (create, retrieve, update, delete) fully functional
- [ ] Database schema runs cleanly on fresh install
- [ ] `config.properties` loads API keys without hardcoding
- [ ] All GRASP pattern comments are present (`// GRASP: Pure Fabrication`, etc.)
