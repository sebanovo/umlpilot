```sql
-- ============================================================
-- DATABASE: UML COLLABORATIVE DESIGNER
-- ENGINE: PostgreSQL
-- AUTHOR: Generated from PlantUML
-- DATE: 2026-09-10
-- ============================================================

-- ============================================================
-- 1. MAIN TABLES
-- ============================================================

-- Table: user
CREATE TABLE "user" (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url VARCHAR(255),
    preferred_language VARCHAR(10),
    theme VARCHAR(20) DEFAULT 'light',
    is_active BOOLEAN DEFAULT TRUE,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_access TIMESTAMP
);

-- Table: project
CREATE TABLE project (
    project_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creator_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'active', -- active, archived, deleted
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES "user"(user_id)
);

-- Table: collaborator (project_collaborator)
CREATE TABLE collaborator (
    project_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL, -- owner, editor, viewer
    permissions JSON,
    invitation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acceptance_date TIMESTAMP,
    invitation_status VARCHAR(20) DEFAULT 'pending', -- pending, accepted, rejected
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES project(project_id),
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- Table: user_session
CREATE TABLE user_session (
    session_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    source_ip VARCHAR(45),
    user_agent TEXT,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiration_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- ============================================================
-- 2. UML ELEMENTS (TREE STRUCTURE)
-- ============================================================

-- Table: element (abstract base class)
CREATE TABLE element (
    element_id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36),
    parent_id VARCHAR(36), -- Recursive relationship for tree structure
    creator_id VARCHAR(36) NOT NULL,
    name VARCHAR(100),
    visibility VARCHAR(10), -- public, private, protected, package
    stereotype VARCHAR(50),
    element_type VARCHAR(30) NOT NULL, -- package, class, interface, enumeration, data_type, primitive_type, note
    position_x INTEGER DEFAULT 0,
    position_y INTEGER DEFAULT 0,
    width INTEGER DEFAULT 100,
    height INTEGER DEFAULT 80,
    properties JSON,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id),
    FOREIGN KEY (parent_id) REFERENCES element(element_id),
    FOREIGN KEY (creator_id) REFERENCES "user"(user_id)
);

-- Table: package (inherits from element)
CREATE TABLE package (
    element_id VARCHAR(36) PRIMARY KEY,
    namespace VARCHAR(255),
    description TEXT,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: class (inherits from element)
CREATE TABLE class (
    element_id VARCHAR(36) PRIMARY KEY,
    is_abstract BOOLEAN DEFAULT FALSE,
    is_final BOOLEAN DEFAULT FALSE,
    base_name VARCHAR(100),
    is_active BOOLEAN DEFAULT FALSE,
    is_template BOOLEAN DEFAULT FALSE,
    template_parameters JSON,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: interface (inherits from element)
CREATE TABLE interface (
    element_id VARCHAR(36) PRIMARY KEY,
    version VARCHAR(20),
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: enumeration (inherits from element)
CREATE TABLE enumeration (
    element_id VARCHAR(36) PRIMARY KEY,
    base_type VARCHAR(50),
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: data_type (inherits from element)
CREATE TABLE data_type (
    element_id VARCHAR(36) PRIMARY KEY,
    primitive_data_type VARCHAR(50),
    is_immutable BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: primitive_type (inherits from element)
CREATE TABLE primitive_type (
    element_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50),
    size INTEGER,
    min_range VARCHAR(50),
    max_range VARCHAR(50),
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: note (inherits from element)
CREATE TABLE note (
    element_id VARCHAR(36) PRIMARY KEY,
    text TEXT,
    background_color VARCHAR(20),
    text_color VARCHAR(20),
    border_style VARCHAR(20),
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- ============================================================
-- 3. INTERNAL STRUCTURE OF ELEMENTS
-- ============================================================

-- Table: attribute
CREATE TABLE attribute (
    attribute_id VARCHAR(36) PRIMARY KEY,
    element_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    visibility VARCHAR(10) DEFAULT 'private', -- public, private, protected, package
    default_value VARCHAR(255),
    is_static BOOLEAN DEFAULT FALSE,
    is_final BOOLEAN DEFAULT FALSE,
    is_transient BOOLEAN DEFAULT FALSE,
    is_volatile BOOLEAN DEFAULT FALSE,
    multiplicity VARCHAR(20),
    order_index INTEGER DEFAULT 0,
    properties JSON,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: method
CREATE TABLE method (
    method_id VARCHAR(36) PRIMARY KEY,
    element_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    return_type VARCHAR(50) NOT NULL,
    visibility VARCHAR(10) DEFAULT 'public',
    is_static BOOLEAN DEFAULT FALSE,
    is_abstract BOOLEAN DEFAULT FALSE,
    is_final BOOLEAN DEFAULT FALSE,
    is_constructor BOOLEAN DEFAULT FALSE,
    is_synchronized BOOLEAN DEFAULT FALSE,
    is_native BOOLEAN DEFAULT FALSE,
    body TEXT,
    order_index INTEGER DEFAULT 0,
    properties JSON,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- Table: parameter
CREATE TABLE parameter (
    parameter_id VARCHAR(36) PRIMARY KEY,
    method_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    order_index INTEGER DEFAULT 0,
    is_varargs BOOLEAN DEFAULT FALSE,
    is_final BOOLEAN DEFAULT FALSE,
    default_value VARCHAR(255),
    FOREIGN KEY (method_id) REFERENCES method(method_id) ON DELETE CASCADE
);

-- Table: literal (for enumerations)
CREATE TABLE literal (
    literal_id VARCHAR(36) PRIMARY KEY,
    element_id VARCHAR(36) NOT NULL, -- reference to enumeration
    name VARCHAR(100) NOT NULL,
    value VARCHAR(255),
    order_index INTEGER DEFAULT 0,
    FOREIGN KEY (element_id) REFERENCES element(element_id) ON DELETE CASCADE
);

-- ============================================================
-- 4. UML RELATIONSHIPS
-- ============================================================

-- Table: relationship
CREATE TABLE relationship (
    relationship_id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL,
    source_element_id VARCHAR(36) NOT NULL,
    target_element_id VARCHAR(36) NOT NULL,
    relationship_type VARCHAR(30) NOT NULL, -- association, generalization, dependency, aggregation, composition, realization, template_binding
    name VARCHAR(100),
    direction VARCHAR(10), -- bidirectional, unidirectional
    is_template_binding BOOLEAN DEFAULT FALSE,
    template_class VARCHAR(100),
    properties JSON,
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id),
    FOREIGN KEY (source_element_id) REFERENCES element(element_id),
    FOREIGN KEY (target_element_id) REFERENCES element(element_id)
);

-- Table: multiplicity
CREATE TABLE multiplicity (
    multiplicity_id VARCHAR(36) PRIMARY KEY,
    relationship_id VARCHAR(36) NOT NULL,
    end VARCHAR(10) NOT NULL, -- source, target
    min INTEGER DEFAULT 0,
    max INTEGER DEFAULT 1,
    is_ordered BOOLEAN DEFAULT FALSE,
    is_unique BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (relationship_id) REFERENCES relationship(relationship_id) ON DELETE CASCADE
);

-- Table: template_binding (for Template Binding)
CREATE TABLE template_binding (
    binding_id VARCHAR(36) PRIMARY KEY,
    relationship_id VARCHAR(36) NOT NULL,
    formal_parameter VARCHAR(100) NOT NULL,
    actual_argument VARCHAR(100) NOT NULL,
    position INTEGER DEFAULT 0,
    FOREIGN KEY (relationship_id) REFERENCES relationship(relationship_id) ON DELETE CASCADE
);

-- Table: association_class (for Association Class)
CREATE TABLE association_class (
    association_class_id VARCHAR(36) PRIMARY KEY,
    relationship_id VARCHAR(36) NOT NULL,
    class_id VARCHAR(36) NOT NULL,
    name VARCHAR(100),
    FOREIGN KEY (relationship_id) REFERENCES relationship(relationship_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES element(element_id)
);

-- ============================================================
-- 5. DIAGRAMS
-- ============================================================

-- Table: diagram
CREATE TABLE diagram (
    diagram_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL, -- class, sequence, use_case, activity, state, etc.
    description TEXT,
    canvas_data JSON,
    version INTEGER DEFAULT 1,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_by_user_id VARCHAR(36),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(project_id),
    FOREIGN KEY (locked_by_user_id) REFERENCES "user"(user_id)
);

-- ============================================================
-- 6. HISTORY AND AUDIT
-- ============================================================

-- Table: change_history
CREATE TABLE change_history (
    change_id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(30) NOT NULL, -- create, edit, delete, move
    affected_object_id VARCHAR(36),
    object_type VARCHAR(20), -- element, attribute, method, relationship
    data_before JSON,
    data_after JSON,
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id),
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- ============================================================
-- 7. ARTIFICIAL INTELLIGENCE (AI)
-- ============================================================

-- Table: ai_command
CREATE TABLE ai_command (
    command_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    diagram_id VARCHAR(36) NOT NULL,
    command_type VARCHAR(20), -- text, voice
    original_command TEXT,
    interpreted_command JSON,
    executed_action JSON,
    success BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    processing_time INTEGER,
    execution_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(user_id),
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id)
);

-- Table: ai_conversation
CREATE TABLE ai_conversation (
    conversation_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    diagram_id VARCHAR(36) NOT NULL,
    type VARCHAR(20), -- assistant, generation
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(user_id),
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id)
);

-- Table: ai_message
CREATE TABLE ai_message (
    message_id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender VARCHAR(10) NOT NULL, -- user, ai
    message TEXT,
    metadata JSON,
    send_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(conversation_id) ON DELETE CASCADE
);

-- ============================================================
-- 8. CODE GENERATION
-- ============================================================

-- Table: code_generation
CREATE TABLE code_generation (
    generation_id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    language VARCHAR(20) NOT NULL, -- springboot
    configuration JSON,
    zip_file_url VARCHAR(255),
    zip_size BIGINT,
    status VARCHAR(20) DEFAULT 'generating', -- generating, completed, error
    error_message TEXT,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_date TIMESTAMP,
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id),
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- ============================================================
-- 9. XMI EXPORT AND IMPORT
-- ============================================================

-- Table: xmi_export
CREATE TABLE xmi_export (
    export_id VARCHAR(36) PRIMARY KEY,
    diagram_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    xmi_version VARCHAR(20),
    file_url VARCHAR(255),
    size BIGINT,
    export_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diagram_id) REFERENCES diagram(diagram_id),
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- Table: xmi_import
CREATE TABLE xmi_import (
    import_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255),
    size BIGINT,
    status VARCHAR(20) DEFAULT 'processing', -- processing, completed, error
    error_message TEXT,
    import_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(project_id),
    FOREIGN KEY (user_id) REFERENCES "user"(user_id)
);

-- ============================================================
-- 10. INDEXES FOR OPTIMIZATION
-- ============================================================

-- Indexes for frequent searches
CREATE INDEX idx_element_diagram ON element(diagram_id);
CREATE INDEX idx_element_parent ON element(parent_id);
CREATE INDEX idx_element_type ON element(element_type);
CREATE INDEX idx_attribute_element ON attribute(element_id);
CREATE INDEX idx_method_element ON method(element_id);
CREATE INDEX idx_parameter_method ON parameter(method_id);
CREATE INDEX idx_literal_element ON literal(element_id);
CREATE INDEX idx_relationship_diagram ON relationship(diagram_id);
CREATE INDEX idx_relationship_source ON relationship(source_element_id);
CREATE INDEX idx_relationship_target ON relationship(target_element_id);
CREATE INDEX idx_multiplicity_relationship ON multiplicity(relationship_id);
CREATE INDEX idx_template_binding_relationship ON template_binding(relationship_id);
CREATE INDEX idx_diagram_project ON diagram(project_id);
CREATE INDEX idx_change_history_diagram ON change_history(diagram_id);
CREATE INDEX idx_change_history_user ON change_history(user_id);
CREATE INDEX idx_ai_command_user ON ai_command(user_id);
CREATE INDEX idx_ai_command_diagram ON ai_command(diagram_id);
CREATE INDEX idx_ai_conversation_user ON ai_conversation(user_id);
CREATE INDEX idx_ai_message_conversation ON ai_message(conversation_id);
CREATE INDEX idx_code_generation_diagram ON code_generation(diagram_id);
CREATE INDEX idx_xmi_export_diagram ON xmi_export(diagram_id);
CREATE INDEX idx_xmi_import_project ON xmi_import(project_id);

-- ============================================================
-- 11. USEFUL VIEWS
-- ============================================================

-- View: complete_elements (with all details)
CREATE VIEW view_complete_elements AS
SELECT 
    e.element_id,
    e.name,
    e.element_type,
    e.visibility,
    e.stereotype,
    e.properties,
    e.diagram_id,
    d.name AS diagram_name,
    d.type AS diagram_type,
    e.parent_id,
    p.name AS parent_name,
    e.creator_id,
    u.username AS creator,
    e.creation_date,
    e.update_date,
    -- Specific fields according to type
    CASE 
        WHEN e.element_type = 'class' THEN c.is_abstract
        ELSE NULL
    END AS is_abstract,
    CASE 
        WHEN e.element_type = 'class' THEN c.is_final
        ELSE NULL
    END AS is_final,
    CASE 
        WHEN e.element_type = 'interface' THEN i.version
        ELSE NULL
    END AS interface_version,
    CASE 
        WHEN e.element_type = 'enumeration' THEN en.base_type
        ELSE NULL
    END AS enumeration_base_type,
    CASE 
        WHEN e.element_type = 'data_type' THEN dt.is_immutable
        ELSE NULL
    END AS is_immutable,
    CASE 
        WHEN e.element_type = 'primitive_type' THEN pt.type
        ELSE NULL
    END AS primitive_type,
    CASE 
        WHEN e.element_type = 'note' THEN n.text
        ELSE NULL
    END AS note_text
FROM element e
LEFT JOIN diagram d ON e.diagram_id = d.diagram_id
LEFT JOIN element p ON e.parent_id = p.element_id
LEFT JOIN "user" u ON e.creator_id = u.user_id
LEFT JOIN class c ON e.element_id = c.element_id AND e.element_type = 'class'
LEFT JOIN interface i ON e.element_id = i.element_id AND e.element_type = 'interface'
LEFT JOIN enumeration en ON e.element_id = en.element_id AND e.element_type = 'enumeration'
LEFT JOIN data_type dt ON e.element_id = dt.element_id AND e.element_type = 'data_type'
LEFT JOIN primitive_type pt ON e.element_id = pt.element_id AND e.element_type = 'primitive_type'
LEFT JOIN note n ON e.element_id = n.element_id AND e.element_type = 'note';

-- View: project_with_collaborators
CREATE VIEW view_project_collaborators AS
SELECT 
    p.project_id,
    p.name AS project_name,
    p.description,
    p.status,
    p.creator_id,
    c_usr.username AS creator_name,
    COUNT(DISTINCT col.user_id) AS total_collaborators,
    json_agg(
        json_build_object(
            'user_id', col.user_id,
            'username', u.username,
            'role', col.role,
            'invitation_status', col.invitation_status
        )
    ) AS collaborators
FROM project p
LEFT JOIN "user" c_usr ON p.creator_id = c_usr.user_id
LEFT JOIN collaborator col ON p.project_id = col.project_id
LEFT JOIN "user" u ON col.user_id = u.user_id
GROUP BY p.project_id, p.name, p.description, p.status, p.creator_id, c_usr.username;

-- View: diagram_with_elements
CREATE VIEW view_diagram_elements AS
SELECT 
    d.diagram_id,
    d.name AS diagram_name,
    d.type AS diagram_type,
    d.project_id,
    pr.name AS project_name,
    COUNT(DISTINCT e.element_id) AS total_elements,
    COUNT(DISTINCT r.relationship_id) AS total_relationships
FROM diagram d
LEFT JOIN project pr ON d.project_id = pr.project_id
LEFT JOIN element e ON d.diagram_id = e.diagram_id
LEFT JOIN relationship r ON d.diagram_id = r.diagram_id
GROUP BY d.diagram_id, d.name, d.type, d.project_id, pr.name;
```