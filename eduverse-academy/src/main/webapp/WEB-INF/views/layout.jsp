<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title><tiles:getAsString name="title" /></title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css' />" />
    <style>
        body { padding-top: 56px; }
        .footer { background-color: #343a40; color: #adb5bd; padding: 30px 0; margin-top: 60px; }
        .hero-section { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 60px 0; }
    </style>
</head>
<body>

<!-- Navigation -->
<nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top">
    <div class="container">
        <a class="navbar-brand" href="<c:url value='/home' />">
            <i class="fas fa-graduation-cap"></i> EduVerse Academy
        </a>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav mr-auto">
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/home' />">Home</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/catalog' />">Courses</a>
                </li>
                <sec:authorize access="hasRole('ROLE_STUDENT')">
                    <li class="nav-item">
                        <a class="nav-link" href="<c:url value='/student/enrollments' />">My Courses</a>
                    </li>
                </sec:authorize>
                <sec:authorize access="hasAnyRole('ROLE_INSTRUCTOR', 'ROLE_ADMIN')">
                    <li class="nav-item">
                        <a class="nav-link" href="<c:url value='/instructor/dashboard' />">Instructor</a>
                    </li>
                </sec:authorize>
                <sec:authorize access="hasRole('ROLE_ADMIN')">
                    <li class="nav-item">
                        <a class="nav-link" href="<c:url value='/admin/dashboard' />">Admin</a>
                    </li>
                </sec:authorize>
            </ul>
            <ul class="navbar-nav">
                <sec:authorize access="isAnonymous()">
                    <li class="nav-item">
                        <a class="nav-link" href="<c:url value='/login' />">Sign In</a>
                    </li>
                </sec:authorize>
                <sec:authorize access="isAuthenticated()">
                    <li class="nav-item">
                        <span class="nav-link text-light">
                            <i class="fas fa-user"></i> <sec:authentication property="principal.username" />
                        </span>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="<c:url value='/logout' />">Sign Out</a>
                    </li>
                </sec:authorize>
            </ul>
        </div>
    </div>
</nav>

<!-- Page Content -->
<main>
    <tiles:insertAttribute name="body" />
</main>

<!-- Footer -->
<footer class="footer">
    <div class="container">
        <div class="row">
            <div class="col-md-4">
                <h5>EduVerse Academy</h5>
                <p>Empowering learners worldwide with high-quality online education.</p>
            </div>
            <div class="col-md-4">
                <h5>Quick Links</h5>
                <ul class="list-unstyled">
                    <li><a href="<c:url value='/catalog' />" class="text-light">Browse Courses</a></li>
                    <li><a href="<c:url value='/about' />" class="text-light">About Us</a></li>
                    <li><a href="<c:url value='/contact' />" class="text-light">Contact</a></li>
                </ul>
            </div>
            <div class="col-md-4">
                <h5>Connect</h5>
                <p>
                    <a href="#" class="text-light mr-3"><i class="fab fa-twitter"></i></a>
                    <a href="#" class="text-light mr-3"><i class="fab fa-linkedin"></i></a>
                    <a href="#" class="text-light"><i class="fab fa-github"></i></a>
                </p>
            </div>
        </div>
        <hr class="bg-secondary">
        <p class="text-center mb-0">&copy; 2024 EduVerse Academy. All rights reserved.</p>
    </div>
</footer>

<script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
<script src="<c:url value='/resources/js/app.js' />"></script>
</body>
</html>
