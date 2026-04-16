-- =============================================================================
-- EduVerse Academy — PostgreSQL Init Script
-- Creates one database per microservice (Database-per-Service pattern)
-- Used by docker-compose postgres-services container
-- =============================================================================

CREATE DATABASE notifications;
CREATE DATABASE coursecatalog;
CREATE DATABASE payments;
CREATE DATABASE videos;
CREATE DATABASE certificates;
CREATE DATABASE assessments;
CREATE DATABASE progress;
CREATE DATABASE enrollments;
