<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!-- Hero Section -->
<div class="hero-section text-center">
    <div class="container">
        <h1 class="display-4">Learn Without Limits</h1>
        <p class="lead">Explore <strong>${courseCount}</strong> courses taught by expert instructors</p>
        <a href="<c:url value='/catalog' />" class="btn btn-light btn-lg mt-3">
            <i class="fas fa-search"></i> Browse Catalog
        </a>
    </div>
</div>

<!-- Featured Courses -->
<div class="container mt-5">
    <h2 class="mb-4">Featured Courses</h2>

    <c:if test="${empty featuredCourses}">
        <div class="alert alert-info">No featured courses available at this time.</div>
    </c:if>

    <div class="row">
        <c:forEach var="course" items="${featuredCourses}">
            <div class="col-md-4 mb-4">
                <div class="card h-100 shadow-sm">
                    <c:choose>
                        <c:when test="${not empty course.thumbnailUrl}">
                            <img src="${course.thumbnailUrl}" class="card-img-top" alt="${course.title}" style="height:180px; object-fit:cover;">
                        </c:when>
                        <c:otherwise>
                            <div class="bg-secondary text-white d-flex align-items-center justify-content-center" style="height:180px;">
                                <i class="fas fa-book fa-3x"></i>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title">${course.title}</h5>
                        <p class="card-text text-muted small">${course.category}</p>
                        <p class="card-text flex-grow-1">
                            <c:out value="${course.description}" />
                        </p>
                        <div class="d-flex justify-content-between align-items-center mt-auto">
                            <span class="h5 text-primary mb-0">
                                $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                            </span>
                            <a href="<c:url value='/course/view/${course.id}' />" class="btn btn-outline-primary">
                                View Course
                            </a>
                        </div>
                    </div>
                    <div class="card-footer text-muted small">
                        <i class="fas fa-clock"></i> ${course.durationHours} hours
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>

    <div class="text-center mt-3 mb-5">
        <a href="<c:url value='/catalog' />" class="btn btn-primary btn-lg">View All Courses</a>
    </div>
</div>
