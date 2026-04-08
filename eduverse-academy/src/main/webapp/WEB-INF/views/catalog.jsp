<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="container mt-4">
    <h2 class="mb-4">Course Catalog</h2>

    <c:if test="${empty courses}">
        <div class="alert alert-info">
            <i class="fas fa-info-circle"></i> No published courses available yet. Check back soon!
        </div>
    </c:if>

    <div class="row">
        <c:forEach var="course" items="${courses}">
            <div class="col-lg-4 col-md-6 mb-4">
                <div class="card h-100 shadow-sm">
                    <c:choose>
                        <c:when test="${not empty course.thumbnailUrl}">
                            <img src="${course.thumbnailUrl}" class="card-img-top" alt="${course.title}" style="height:180px; object-fit:cover;">
                        </c:when>
                        <c:otherwise>
                            <div class="bg-info text-white d-flex align-items-center justify-content-center" style="height:180px;">
                                <i class="fas fa-chalkboard-teacher fa-3x"></i>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="card-body d-flex flex-column">
                        <span class="badge badge-secondary mb-2" style="width:fit-content;">${course.category}</span>
                        <h5 class="card-title">${course.title}</h5>
                        <p class="card-text flex-grow-1">
                            <c:out value="${course.description}" />
                        </p>
                        <div class="d-flex justify-content-between align-items-center mt-auto">
                            <span class="h5 text-success mb-0">
                                $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                            </span>
                            <a href="<c:url value='/course/view/${course.id}' />" class="btn btn-primary btn-sm">
                                <i class="fas fa-eye"></i> View Details
                            </a>
                        </div>
                    </div>
                    <div class="card-footer text-muted small d-flex justify-content-between">
                        <span><i class="fas fa-clock"></i> ${course.durationHours} hrs</span>
                        <span><i class="fas fa-layer-group"></i> ${course.modules.size()} modules</span>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</div>
