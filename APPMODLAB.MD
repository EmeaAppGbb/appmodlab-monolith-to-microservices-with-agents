---
title: "Monolith to Microservices with Agents"
description: "Decompose Java monolith to microservices using Spec2Cloud & SQUAD with Strangler Fig pattern"
authors: ["marconsilva"]
category: "Code Modernization"
industry: "Education"
services: ["Azure Container Apps", "Azure Service Bus", "Azure API Management", "Azure SQL Database"]
languages: ["Java"]
frameworks: ["Spring Boot", "Spring Cloud Gateway"]
modernizationTools: []
agenticTools: ["Spec2Cloud", "SQUAD"]
tags: ["microservices", "strangler-fig", "bounded-context", "saga-pattern", "spring-boot"]
extensions: ["github.copilot"]
thumbnail: ""
video: ""
version: "1.0.0"
---

## Initial Application Screenshots

The following screenshots show the EduVerse Academy monolith running locally via Docker Compose (Tomcat 9 + PostgreSQL 14).

### Homepage

The landing page features a hero banner, featured courses section, and navigation bar with links to the course catalog, sign-in, and more.

![Homepage](assets/screenshots/01-homepage.png)

### Login Page

Spring Security form-based authentication with username/password fields and a "Remember me" option.

![Login Page](assets/screenshots/02-login-page.png)

### Course Catalog

The course catalog page (empty database — no courses seeded yet). This is one of the 8 bounded contexts targeted for extraction.

![Course Catalog](assets/screenshots/03-course-catalog.png)
