<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2><i class="fas fa-chalkboard-teacher"></i> Instructor Dashboard</h2>
        <a href="<c:url value='/course/edit/new' />" class="btn btn-success">
            <i class="fas fa-plus"></i> Create New Course
        </a>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <!-- Summary Cards -->
    <div class="row mb-4">
        <div class="col-md-4">
            <div class="card bg-primary text-white shadow">
                <div class="card-body text-center">
                    <h3>${totalCourses}</h3>
                    <p class="mb-0">Total Courses</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card bg-success text-white shadow">
                <div class="card-body text-center">
                    <h3>
                        <c:set var="totalEnrolled" value="0" />
                        <c:forEach var="entry" items="${enrollmentCounts}">
                            <c:set var="totalEnrolled" value="${totalEnrolled + entry.value}" />
                        </c:forEach>
                        ${totalEnrolled}
                    </h3>
                    <p class="mb-0">Total Enrollments</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card bg-info text-white shadow">
                <div class="card-body text-center">
                    <h3>
                        <c:set var="publishedCount" value="0" />
                        <c:forEach var="course" items="${courses}">
                            <c:if test="${course.status == 'PUBLISHED'}">
                                <c:set var="publishedCount" value="${publishedCount + 1}" />
                            </c:if>
                        </c:forEach>
                        ${publishedCount}
                    </h3>
                    <p class="mb-0">Published</p>
                </div>
            </div>
        </div>
    </div>

    <!-- Courses Table -->
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">My Courses</h5>
        </div>
        <div class="card-body p-0">
            <c:if test="${empty courses}">
                <div class="p-4 text-center text-muted">
                    You haven't created any courses yet. Click "Create New Course" to get started.
                </div>
            </c:if>
            <c:if test="${not empty courses}">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="thead-light">
                            <tr>
                                <th>Title</th>
                                <th>Category</th>
                                <th>Status</th>
                                <th>Enrollments</th>
                                <th>Price</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="course" items="${courses}">
                                <tr>
                                    <td>
                                        <a href="<c:url value='/course/view/${course.id}' />">
                                            ${course.title}
                                        </a>
                                    </td>
                                    <td><span class="badge badge-secondary">${course.category}</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${course.status == 'PUBLISHED'}">
                                                <span class="badge badge-success">Published</span>
                                            </c:when>
                                            <c:when test="${course.status == 'DRAFT'}">
                                                <span class="badge badge-warning">Draft</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-secondary">${course.status}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:out value="${enrollmentCounts[course.id]}" default="0" />
                                    </td>
                                    <td>$<fmt:formatNumber value="${course.price}" pattern="#,##0.00" /></td>
                                    <td>
                                        <a href="<c:url value='/course/edit/${course.id}' />"
                                           class="btn btn-sm btn-outline-primary" title="Edit">
                                            <i class="fas fa-edit"></i>
                                        </a>
                                        <a href="<c:url value='/instructor/analytics/${course.id}' />"
                                           class="btn btn-sm btn-outline-info" title="Analytics">
                                            <i class="fas fa-chart-bar"></i>
                                        </a>
                                        <c:if test="${course.status == 'DRAFT'}">
                                            <form action="<c:url value='/course/publish/${course.id}' />"
                                                  method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success" title="Publish">
                                                    <i class="fas fa-upload"></i>
                                                </button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
        </div>
    </div>
</div>
