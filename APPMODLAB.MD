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

## Overview

This lab decomposes a Java monolith into microservices using Spec2Cloud and SQUAD with the Strangler Fig pattern. The target application is EduVerse Academy, an education platform built on Tomcat 9 with PostgreSQL 14.

## Understanding the Monolith

The following sections show the EduVerse Academy monolith running locally via Docker Compose (Tomcat 9 + PostgreSQL 14). Understanding the current application is essential before identifying bounded contexts for microservice extraction.

The landing page features a hero banner, featured courses section, and navigation bar with links to the course catalog, sign-in, and more:

![Homepage](assets/screenshots/01-homepage.png)

Here is a wider view of the full homepage layout including all navigation elements and content sections:

![Homepage Full View](assets/screenshots/05-homepage-full.png)

![Homepage Alternate View](assets/screenshots/04-home-page.png)

### Authentication

Spring Security form-based authentication with username/password fields and a "Remember me" option:

![Login Page](assets/screenshots/02-login-page.png)

### Course Catalog — Bounded Context Example

The course catalog page (empty database — no courses seeded yet). This is one of the 8 bounded contexts targeted for extraction into a standalone microservice:

![Course Catalog](assets/screenshots/03-course-catalog.png)

## Screenshots Reference

| Screenshot | Description |
|---|---|
| ![Homepage](assets/screenshots/01-homepage.png) | Main landing page |
| ![Login Page](assets/screenshots/02-login-page.png) | Spring Security login |
| ![Course Catalog](assets/screenshots/03-course-catalog.png) | Course catalog (bounded context) |
| ![Homepage Alt](assets/screenshots/04-home-page.png) | Homepage alternate view |
| ![Homepage Full](assets/screenshots/05-homepage-full.png) | Full homepage layout |
