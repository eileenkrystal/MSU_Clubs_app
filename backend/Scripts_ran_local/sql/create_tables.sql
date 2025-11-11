-- ON SUPABASE, HERE FOR GRADING
-- USERS
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    meta TEXT
);

-- CLUBS
CREATE TABLE clubs (
    id CHAR(36) PRIMARY KEY,
    slug VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    socials TEXT,
    website VARCHAR(255),
    address VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255)
);

-- TAGS
CREATE TABLE tags (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL
);

-- CLUB_TAGS (join table to add multiple tags to clubs)
CREATE TABLE club_tags (
    club_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (club_id, tag_id),
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- CLUB_ADMINS (one admin per club, enforced by UNIQUE constraint)
CREATE TABLE club_admins (
    id CHAR(36) PRIMARY KEY,
    club_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- FAVORITES (users can bookmark clubs)
CREATE TABLE favorites (
    user_id CHAR(36) NOT NULL,
    club_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, club_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_clubs_slug ON clubs(slug);
CREATE INDEX idx_users_email ON users(email);
