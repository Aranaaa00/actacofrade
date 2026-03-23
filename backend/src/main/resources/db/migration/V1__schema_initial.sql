-- ACTACOFRADE - DATABASE SCHEMA (POSTGRESQL) - V1

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- === 1. DATA TYPES (ENUMS) ===

-- User roles
CREATE TYPE role_code AS ENUM ('ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA');

-- Event/Expedient status phases
CREATE TYPE event_status AS ENUM ('PLANIFICACION', 'PREPARACION', 'CONFIRMACION', 'CIERRE', 'CERRADO');

-- Event classifications
CREATE TYPE event_type AS ENUM ('CABILDO', 'CULTOS', 'PROCESION', 'ENSAYO', 'OTRO');

-- Task completion states
CREATE TYPE task_status AS ENUM ('PENDIENTE', 'CONFIRMADA', 'RECHAZADA');

-- Brotherhood organizational areas (for Decisions)
CREATE TYPE hermandad_area AS ENUM ('MAYORDOMIA', 'SECRETARIA', 'PRIOSTIA', 'TESORERIA', 'DIPUTACION_MAYOR');

-- Decision states
CREATE TYPE decision_status AS ENUM ('PENDIENTE', 'LISTA', 'RECHAZADA');

-- Incident states
CREATE TYPE incident_status AS ENUM ('ABIERTA', 'RESUELTA');


-- === 2. SECURITY & USERS MODULE ===

-- Access levels definition
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    code role_code NOT NULL UNIQUE,  
    description VARCHAR(255)         
);

-- Board members or brotherhood users
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,           
    email VARCHAR(255) NOT NULL UNIQUE,        
    password_hash VARCHAR(255) NOT NULL,       
    active BOOLEAN DEFAULT TRUE,               -- Soft delete flag
    last_login TIMESTAMP,                      
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- N:M relationship for user roles
CREATE TABLE user_roles (
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);


-- === 3. EVENTS MODULE (EXPEDIENTES) ===

-- Main events, cults and processes records
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(20) UNIQUE NOT NULL,     -- Auto-generated code (e.g. "2026/0042")
    title VARCHAR(255) NOT NULL,               
    event_type event_type NOT NULL,            
    event_date DATE NOT NULL,                  
    location VARCHAR(255),                     
    observations TEXT,                         
    status event_status DEFAULT 'PLANIFICACION', 
    responsible_id INTEGER REFERENCES users(id), 
    is_locked_for_closing BOOLEAN DEFAULT FALSE, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Traceability for the "Clone Event" feature
CREATE TABLE event_clones (
    original_event_id INTEGER REFERENCES events(id),
    cloned_event_id INTEGER REFERENCES events(id),
    cloned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (original_event_id, cloned_event_id)
);


-- === 4. TASKS, INCIDENTS & DECISIONS MODULE ===

-- Assignments required for event execution
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    event_id INTEGER REFERENCES events(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,               
    description TEXT,                          
    assigned_to INTEGER REFERENCES users(id),  
    status task_status DEFAULT 'PENDIENTE',
    deadline DATE,                             
    rejection_reason TEXT,                     -- Required when status is RECHAZADA
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Alerts or blocking issues that prevent event closing
CREATE TABLE incidents (
    id SERIAL PRIMARY KEY,
    event_id INTEGER REFERENCES events(id) ON DELETE CASCADE,
    description TEXT NOT NULL,                 
    status incident_status DEFAULT 'ABIERTA',
    reported_by INTEGER REFERENCES users(id),  
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- Agreements or votes classified by brotherhood areas
CREATE TABLE decisions (
    id SERIAL PRIMARY KEY,
    event_id INTEGER REFERENCES events(id) ON DELETE CASCADE,
    area hermandad_area NOT NULL,              
    title VARCHAR(255) NOT NULL,               
    status decision_status DEFAULT 'PENDIENTE',
    reviewed_by INTEGER REFERENCES users(id),  
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);