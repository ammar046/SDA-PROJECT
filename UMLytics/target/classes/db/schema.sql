-- UMLytics Database Schema (SQLite)

CREATE TABLE IF NOT EXISTS projects (
    project_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE,
    description   TEXT,
    created_date  TEXT NOT NULL,
    last_modified TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS diagrams (
    diagram_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id    INTEGER NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    title         TEXT NOT NULL,
    source_type   TEXT NOT NULL,
    model_json    TEXT,
    created_date  TEXT NOT NULL,
    last_modified TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS uml_classes (
    class_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id   INTEGER NOT NULL REFERENCES diagrams(diagram_id) ON DELETE CASCADE,
    name         TEXT NOT NULL,
    is_abstract  INTEGER DEFAULT 0,
    is_interface INTEGER DEFAULT 0,
    position_x   REAL DEFAULT 100,
    position_y   REAL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS attributes (
    attr_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id   INTEGER NOT NULL REFERENCES uml_classes(class_id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    type       TEXT NOT NULL,
    visibility TEXT NOT NULL DEFAULT 'PRIVATE',
    is_static  INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS methods (
    method_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id    INTEGER NOT NULL REFERENCES uml_classes(class_id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    return_type TEXT NOT NULL DEFAULT 'void',
    parameters  TEXT DEFAULT '',
    visibility  TEXT NOT NULL DEFAULT 'PUBLIC',
    is_abstract INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS relationships (
    rel_id              INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id          INTEGER NOT NULL REFERENCES diagrams(diagram_id) ON DELETE CASCADE,
    source_class_id     INTEGER NOT NULL,
    target_class_id     INTEGER NOT NULL,
    rel_type            TEXT NOT NULL,
    source_multiplicity TEXT DEFAULT '1',
    target_multiplicity TEXT DEFAULT '*',
    label               TEXT DEFAULT '',
    is_composition      INTEGER DEFAULT 0,
    is_aggregation      INTEGER DEFAULT 0,
    navigability        TEXT DEFAULT 'UNIDIRECTIONAL',
    dependency_type     TEXT DEFAULT '',
    is_interface_rel    INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    sender     TEXT NOT NULL,
    timestamp  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS evaluation_reports (
    report_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id     INTEGER NOT NULL REFERENCES diagrams(diagram_id) ON DELETE CASCADE,
    project_id     INTEGER NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    coupling_score REAL DEFAULT 0,
    cohesion_score REAL DEFAULT 0,
    solid_score    REAL DEFAULT 0,
    suggestions    TEXT DEFAULT '[]',
    generated_date TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS structure_suggestions (
    suggestion_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id     INTEGER NOT NULL REFERENCES diagrams(diagram_id) ON DELETE CASCADE,
    code_skeletons TEXT DEFAULT '{}',
    generated_date TEXT NOT NULL
);
