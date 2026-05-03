CREATE TABLE IF NOT EXISTS project (
    project_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_date TEXT,
    last_modified TEXT
);

CREATE TABLE IF NOT EXISTS uml_diagram (
    diagram_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    source_type TEXT CHECK(source_type IN ('NATURAL_LANGUAGE','SOURCE_CODE','MANUAL','UPLOADED_IMAGE')),
    render_state TEXT CHECK(render_state IN ('PENDING','RENDERING','RENDERED','ERROR')) DEFAULT 'PENDING',
    last_updated TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS conceptual_class (
    class_id TEXT PRIMARY KEY,
    diagram_id TEXT NOT NULL REFERENCES uml_diagram(diagram_id) ON DELETE CASCADE,
    class_name TEXT NOT NULL,
    class_type TEXT CHECK(class_type IN ('ENTITY','ABSTRACT','INTERFACE','ENUM','PACKAGE')) DEFAULT 'ENTITY',
    visibility TEXT CHECK(visibility IN ('PUBLIC','PRIVATE','PROTECTED','PACKAGE')) DEFAULT 'PUBLIC',
    position_x REAL DEFAULT 100,
    position_y REAL DEFAULT 100,
    header_color TEXT DEFAULT 'Blue',
    border_color TEXT DEFAULT 'Blue',
    member_font_size REAL DEFAULT 12.0,
    class_width REAL DEFAULT 200,
    class_height REAL DEFAULT 140
);

CREATE TABLE IF NOT EXISTS attribute (
    attribute_id TEXT PRIMARY KEY,
    class_id TEXT NOT NULL REFERENCES conceptual_class(class_id) ON DELETE CASCADE,
    attribute_name TEXT NOT NULL,
    data_type TEXT,
    default_value TEXT,
    visibility TEXT DEFAULT 'PRIVATE'
);

CREATE TABLE IF NOT EXISTS method (
    method_id TEXT PRIMARY KEY,
    class_id TEXT NOT NULL REFERENCES conceptual_class(class_id) ON DELETE CASCADE,
    method_name TEXT NOT NULL,
    return_type TEXT,
    parameters TEXT,
    visibility TEXT DEFAULT 'PUBLIC'
);

CREATE TABLE IF NOT EXISTS relationship (
    relationship_id TEXT PRIMARY KEY,
    diagram_id TEXT NOT NULL REFERENCES uml_diagram(diagram_id) ON DELETE CASCADE,
    relationship_type TEXT,
    source_class_id TEXT NOT NULL REFERENCES conceptual_class(class_id),
    target_class_id TEXT NOT NULL REFERENCES conceptual_class(class_id),
    source_multiplicity TEXT,
    target_multiplicity TEXT,
    label TEXT,
    bend_x REAL,
    edge_color TEXT DEFAULT 'Black',
    dashed INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_message (
    message_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    class_id TEXT REFERENCES conceptual_class(class_id),
    sender_role TEXT CHECK(sender_role IN ('USER','AI','SYSTEM')),
    content TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS design_evaluation_report (
    report_id TEXT PRIMARY KEY,
    diagram_id TEXT REFERENCES uml_diagram(diagram_id) ON DELETE CASCADE,
    project_id TEXT REFERENCES project(project_id),
    coupling_score REAL,
    cohesion_score REAL,
    solid_score REAL,
    feedback_summary TEXT,
    evaluation_date TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS class_suggestion (
    suggestion_id TEXT PRIMARY KEY,
    class_id TEXT REFERENCES conceptual_class(class_id),
    diagram_id TEXT REFERENCES uml_diagram(diagram_id) ON DELETE CASCADE,
    skeleton_code TEXT,
    explanation TEXT,
    accepted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS diagram_image (
    image_id TEXT PRIMARY KEY,
    diagram_id TEXT REFERENCES uml_diagram(diagram_id) ON DELETE CASCADE,
    file_name TEXT,
    image_format TEXT CHECK(image_format IN ('PNG','JPG','JPEG')),
    upload_date TEXT
);

CREATE TABLE IF NOT EXISTS source_file (
    file_id TEXT PRIMARY KEY,
    project_id TEXT REFERENCES project(project_id) ON DELETE CASCADE,
    diagram_id TEXT REFERENCES uml_diagram(diagram_id),
    file_name TEXT,
    file_content TEXT,
    language TEXT CHECK(language IN ('JAVA','PYTHON','CPP','CSHARP','OTHER'))
);
