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
    source_type TEXT NOT NULL CHECK(source_type IN ('NL','CODE','UPLOAD','MANUAL')),
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
    header_color TEXT DEFAULT 'Blue',
    border_color TEXT DEFAULT 'Blue',
    member_font_size REAL DEFAULT 12,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attributes (
    attribute_id INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    visibility TEXT NOT NULL CHECK(visibility IN ('PUBLIC','PRIVATE','PROTECTED','PACKAGE')),
    is_static INTEGER DEFAULT 0,
    FOREIGN KEY (class_id) REFERENCES uml_classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS methods (
    method_id INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    return_type TEXT NOT NULL,
    parameters TEXT,
    visibility TEXT NOT NULL CHECK(visibility IN ('PUBLIC','PRIVATE','PROTECTED','PACKAGE')),
    is_abstract INTEGER DEFAULT 0,
    FOREIGN KEY (class_id) REFERENCES uml_classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS relationships (
    relationship_id INTEGER PRIMARY KEY AUTOINCREMENT,
    diagram_id INTEGER NOT NULL,
    source_class_id INTEGER NOT NULL,
    target_class_id INTEGER NOT NULL,
    relationship_type TEXT NOT NULL CHECK(relationship_type IN ('INHERITANCE','COMPOSITION','AGGREGATION','DEPENDENCY','REALIZATION','ASSOCIATION')),
    source_multiplicity TEXT,
    target_multiplicity TEXT,
    label TEXT,
    is_composition INTEGER DEFAULT 0,
    is_aggregation INTEGER DEFAULT 0,
    is_interface INTEGER DEFAULT 0,
    navigability TEXT DEFAULT 'BIDIRECTIONAL',
    dependency_type TEXT,
    bend_x REAL,
    edge_color TEXT DEFAULT 'Black',
    dashed INTEGER DEFAULT 0,
    FOREIGN KEY (diagram_id) REFERENCES uml_diagrams(diagram_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    sender TEXT NOT NULL CHECK(sender IN ('USER','AI')),
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
