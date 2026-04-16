-- Course Catalog Service — Seed Data
-- Migrated from monolith data.sql (courses, modules, lessons only)

-- Courses (5 published + 1 draft + 1 archived = 7 courses)
INSERT INTO courses (id, title, description, instructor_id, category, price, status, published_date, duration_hours, thumbnail_url, created_at, updated_at) VALUES
(1, 'Introduction to Java Programming',
    'A comprehensive beginner course covering Java fundamentals, OOP principles, collections, and basic I/O. Build real-world projects along the way.',
    2, 'Programming', 49.99, 'PUBLISHED', '2024-02-15 10:00:00', 40, NULL, '2024-02-01 09:00:00', '2024-02-15 10:00:00'),

(2, 'Data Science with Python',
    'Learn data analysis, visualization, and machine learning using Python, Pandas, Matplotlib, and scikit-learn.',
    3, 'Data Science', 79.99, 'PUBLISHED', '2024-03-01 12:00:00', 60, NULL, '2024-02-20 14:00:00', '2024-03-01 12:00:00'),

(3, 'Cloud Architecture on Azure',
    'Design scalable and resilient cloud solutions with Microsoft Azure. Covers compute, storage, networking, and security best practices.',
    2, 'Cloud Computing', 99.99, 'PUBLISHED', '2024-03-20 08:00:00', 35, NULL, '2024-03-10 11:00:00', '2024-03-20 08:00:00'),

(4, 'UX Design Fundamentals',
    'Master user-centered design thinking, wireframing, prototyping, and usability testing.',
    3, 'Design', 59.99, 'DRAFT', NULL, 25, NULL, '2024-04-01 09:00:00', '2024-04-01 09:00:00'),

(5, 'DevOps and CI/CD Pipelines',
    'Automate builds, tests, and deployments with Git, Jenkins, Docker, and Kubernetes.',
    2, 'DevOps', 89.99, 'PUBLISHED', '2024-01-20 08:00:00', 45, NULL, '2024-01-10 08:00:00', '2024-04-15 17:00:00'),

(6, 'Full-Stack Web Development',
    'Build modern web applications with React, Node.js, and PostgreSQL from scratch.',
    2, 'Programming', 69.99, 'PUBLISHED', '2024-05-01 10:00:00', 55, NULL, '2024-04-20 09:00:00', '2024-05-01 10:00:00'),

(7, 'Cybersecurity Essentials',
    'Understand threats, vulnerabilities, encryption, and network security fundamentals.',
    3, 'Security', 74.99, 'ARCHIVED', '2024-02-01 08:00:00', 30, NULL, '2024-01-15 10:00:00', '2024-06-01 12:00:00');

-- Modules
-- Course 1 – Java Programming
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(1, 1, 'Getting Started with Java',   1, 'Install the JDK, set up your IDE, and write your first program.'),
(2, 1, 'Object-Oriented Programming', 2, 'Classes, objects, inheritance, polymorphism, and encapsulation.'),
(3, 1, 'Collections and Generics',    3, 'Lists, maps, sets, and type-safe collections.');

-- Course 2 – Data Science
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(4, 2, 'Python Essentials',         1, 'Variables, control flow, functions, and data structures.'),
(5, 2, 'Data Analysis with Pandas', 2, 'DataFrames, data cleaning, grouping, and merging.'),
(6, 2, 'Machine Learning Basics',   3, 'Supervised vs. unsupervised learning, model training, evaluation.');

-- Course 3 – Cloud Architecture
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(7, 3, 'Azure Fundamentals',    1, 'Core Azure services, regions, and resource groups.'),
(8, 3, 'Compute and Networking', 2, 'Virtual machines, App Service, VNets, and load balancers.');

-- Course 5 – DevOps
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(9,  5, 'Version Control with Git', 1, 'Branching, merging, pull requests, and Git workflows.'),
(10, 5, 'Containers and Docker',    2, 'Building images, running containers, and Docker Compose.');

-- Course 6 – Full-Stack Web Development
INSERT INTO modules (id, course_id, title, sort_order, description) VALUES
(11, 6, 'HTML, CSS, and JavaScript', 1, 'Web fundamentals and responsive design.'),
(12, 6, 'React Frontend',            2, 'Components, state management, and hooks.'),
(13, 6, 'Node.js Backend',           3, 'Express APIs, middleware, and database integration.');

-- Lessons
-- Module 1 – Getting Started with Java
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(1,  1, 'Installing Java and IntelliJ', 'VIDEO',    'https://videos.eduverse.io/java/lesson1.mp4', 15, 1, NULL),
(2,  1, 'Hello World Program',          'DOCUMENT', NULL, 10, 2, 'Create a class with a main method and print a greeting to the console.'),
(3,  1, 'Variables and Data Types',     'VIDEO',    'https://videos.eduverse.io/java/lesson3.mp4', 20, 3, NULL),
(4,  1, 'Module 1 Quiz',               'QUIZ',     NULL,  5, 4, NULL);

-- Module 2 – OOP
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(5,  2, 'Classes and Objects', 'VIDEO',      'https://videos.eduverse.io/java/lesson5.mp4', 25, 1, NULL),
(6,  2, 'Inheritance',         'VIDEO',      'https://videos.eduverse.io/java/lesson6.mp4', 20, 2, NULL),
(7,  2, 'OOP Assignment',      'ASSIGNMENT', NULL, 30, 3, 'Build a simple bank-account class hierarchy with deposits and withdrawals.');

-- Module 3 – Collections
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(8,  3, 'Lists and Iterators', 'VIDEO', 'https://videos.eduverse.io/java/lesson8.mp4', 18, 1, NULL),
(9,  3, 'Maps and Sets',       'VIDEO', 'https://videos.eduverse.io/java/lesson9.mp4', 22, 2, NULL);

-- Module 4 – Python Essentials
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(10, 4, 'Python Setup and Syntax', 'VIDEO',    'https://videos.eduverse.io/python/lesson10.mp4', 12, 1, NULL),
(11, 4, 'Functions and Modules',   'DOCUMENT', NULL, 15, 2, 'Learn to define reusable functions and organize code into modules.');

-- Module 5 – Pandas
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(12, 5, 'Introduction to Pandas', 'VIDEO',      'https://videos.eduverse.io/python/lesson12.mp4', 20, 1, NULL),
(13, 5, 'Data Cleaning Lab',      'ASSIGNMENT', NULL, 40, 2, 'Clean a messy CSV dataset using Pandas.');

-- Module 6 – ML Basics
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(14, 6, 'Supervised Learning Overview', 'VIDEO', 'https://videos.eduverse.io/python/lesson14.mp4', 25, 1, NULL),
(15, 6, 'ML Quiz',                      'QUIZ',  NULL, 10, 2, NULL);

-- Module 7 – Azure Fundamentals
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(16, 7, 'What is Cloud Computing?', 'VIDEO', 'https://videos.eduverse.io/azure/lesson16.mp4', 15, 1, NULL),
(17, 7, 'Azure Portal Walkthrough', 'VIDEO', 'https://videos.eduverse.io/azure/lesson17.mp4', 20, 2, NULL);

-- Module 8 – Compute and Networking
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(18, 8, 'Virtual Machines',     'VIDEO',      'https://videos.eduverse.io/azure/lesson18.mp4', 22, 1, NULL),
(19, 8, 'Azure Networking Lab', 'ASSIGNMENT', NULL, 45, 2, 'Deploy a VNet with two subnets and a network security group.');

-- Module 9 – Git
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(20, 9, 'Git Basics',       'VIDEO',    'https://videos.eduverse.io/devops/lesson20.mp4', 18, 1, NULL),
(21, 9, 'Branching and PRs', 'DOCUMENT', NULL, 15, 2, 'Learn feature branches, pull requests, and code review workflows.');

-- Module 10 – Docker
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(22, 10, 'Docker Fundamentals', 'VIDEO',      'https://videos.eduverse.io/devops/lesson22.mp4', 25, 1, NULL),
(23, 10, 'Docker Lab',          'ASSIGNMENT', NULL, 35, 2, 'Containerize a Java web application with a multi-stage Dockerfile.');

-- Module 11 – HTML/CSS/JS
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(24, 11, 'HTML and CSS Basics', 'VIDEO', 'https://videos.eduverse.io/web/lesson24.mp4', 20, 1, NULL),
(25, 11, 'JavaScript Essentials', 'VIDEO', 'https://videos.eduverse.io/web/lesson25.mp4', 22, 2, NULL);

-- Module 12 – React
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(26, 12, 'React Components',   'VIDEO', 'https://videos.eduverse.io/web/lesson26.mp4', 25, 1, NULL),
(27, 12, 'State and Hooks',    'VIDEO', 'https://videos.eduverse.io/web/lesson27.mp4', 20, 2, NULL);

-- Module 13 – Node.js
INSERT INTO lessons (id, module_id, title, content_type, video_url, duration_minutes, sort_order, content) VALUES
(28, 13, 'Express.js REST APIs', 'VIDEO',      'https://videos.eduverse.io/web/lesson28.mp4', 22, 1, NULL),
(29, 13, 'Full-Stack Project',   'ASSIGNMENT', NULL, 60, 2, 'Build a complete CRUD application with React + Express + PostgreSQL.');

-- Reset sequences to avoid conflicts with future inserts
SELECT setval('courses_id_seq', (SELECT MAX(id) FROM courses));
SELECT setval('modules_id_seq', (SELECT MAX(id) FROM modules));
SELECT setval('lessons_id_seq', (SELECT MAX(id) FROM lessons));
