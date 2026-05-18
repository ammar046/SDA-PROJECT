# UMLytics

UMLytics is an AI-powered UML class diagram design environment built with JavaFX. It allows users to generate, edit, evaluate, and export UML class diagrams using:

- natural language prompts
- Java source code parsing
- image-based diagram analysis
- interactive diagram editing
- AI-assisted chat and design evaluation

The application also stores projects, diagrams, chat history, and evaluation reports in a local SQLite database.

## Key Features

- Generate UML diagrams from natural-language design descriptions
- Reverse-engineer UML diagrams from Java source files
- Analyze uploaded diagram images using vision-enabled AI
- Edit diagram elements interactively in a JavaFX canvas
- Evaluate design quality using AI-generated coupling, cohesion, and SOLID feedback
- Persist projects, diagrams, chat history, and AI suggestions to SQLite
- Export diagrams to PNG, SVG, and PDF formats
- Speech-to-text support for the AI chat sidebar

## Architecture

UMLytics follows a layered architecture with clear separation of concerns:

- `ui` / `ui.panels`: JavaFX presentation layer and application shell
- `controllers`: orchestration layer for project, diagram, and AI workflows
- `services`: external integration with AI, code parsing, export, and speech-to-text
- `repository`: persistence interfaces and SQLite-backed implementations
- `domain`: UML model objects, diagrams, projects, relationships, and chat/report payloads
- `db`: SQLite database initialization and connection management

The root entry point is `com.umlytics.Main`, which launches the JavaFX `MainWindow`.

## Technology Stack

- Java 17
- JavaFX 21
- Maven build system
- SQLite via `sqlite-jdbc`
- OkHttp for HTTP communication
- Gson for JSON handling
- JavaParser for Java source analysis
- Apache PDFBox and Batik SVG for export
- JNA for native integration
- JUnit Jupiter for tests

## Project Structure

- `src/main/java/com/umlytics`: main application code
- `src/main/resources`: configuration example and database schema
- `src/test/java/com/umlytics`: unit and integration tests
- `pom.xml`: Maven build configuration
- `README.md`: this file

## Getting Started

### Prerequisites

- Java 17 SDK installed
- Maven installed (`mvn` command available)
- Internet access for Maven dependency download

### Configuration

1. Copy the example configuration file:

   ```bash
   cp src/main/resources/config.properties.example config.properties
   ```

2. Edit `config.properties` and add your AI API key and optional endpoints.

   Required values:

   - `db.url=jdbc:sqlite:umlytics.db`
   - `ai.api.key=YOUR_API_KEY_HERE`
   - `ai.api.endpoint=https://api.cerebras.ai/v1/chat/completions`
   - `ai.api.model=llama3.1-8b`

3. Optionally set environment variables instead of storing secrets in the file:

   - `UMLYTICS_API_KEY`
   - `OPENAI_API_KEY`
   - `CEREBRAS_API_KEY`
   - `GEMINI_API_KEY`

### Build

From the project root:

```bash
mvn clean package
```

### Run

Run the application using the JavaFX Maven plugin:

```bash
mvn clean javafx:run
```

Alternatively, run the main class directly from your IDE:

- Main class: `com.umlytics.Main`

### Test

Execute the test suite:

```bash
mvn test
```

## Database

The application initializes a local SQLite database using `src/main/resources/schema.sql` on first launch. The default database file is `umlytics.db`.

## Configuration Notes

- `config.properties` may be placed at the project root or loaded from the resource path.
- The example includes optional settings for vision endpoints and speech-to-text providers.
- Export defaults are configurable via `export.default.path`.
- UI preferences such as theme and canvas zoom are also available in the configuration file.

## How It Works

### Diagram generation flows

- `ProjectController`: creates and manages project workspaces
- `DiagramController`: handles diagram generation, modification, export, and persistence
- `AIController`: handles AI requests, design chat, and evaluation

### AI integration

- `LLMAPIEngine` performs HTTP calls to the configured AI endpoint
- `JavaCodeParser` parses Java source files and converts class declarations into UML domain objects
- `DiagramExportService` produces PNG, SVG, and PDF output

## Notes for Developers

- The JavaFX UI is initialized in `com.umlytics.ui.MainWindow`
- The application uses a `DatabaseManager` singleton for SQLite setup
- Repositories are interfaces implemented in `com.umlytics.repository`
- Domain objects are defined in `com.umlytics.domain`
- Add new AI models or endpoints by extending `LLMAPIEngine` and updating configuration handling

## Useful Commands

- Build project: `mvn clean package`
- Run app: `mvn clean javafx:run`
- Run tests: `mvn test`

## Contributors

This repository is organized to support development, research, and extension of AI-enhanced UML design tooling. The codebase documentation files `codebase_documentation.md` and `UMLytics_Masterclass.md` provide deeper architectural and design pattern explanations.
