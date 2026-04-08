<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="container mt-4">
    <h2 class="mb-4"><i class="fas fa-book-reader"></i> My Enrolled Courses</h2>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <c:if test="${empty enrollments}">
        <div class="alert alert-info">
            <i class="fas fa-info-circle"></i> You haven't enrolled in any courses yet.
            <a href="<c:url value='/catalog' />" class="alert-link">Browse the catalog</a> to get started!
        </div>
    </c:if>

    <div class="row">
        <c:forEach var="enrollment" items="${enrollments}">
            <div class="col-md-6 mb-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h5 class="card-title mb-0">
                                <c:out value="${enrollment.courseId}" />
                            </h5>
                            <c:choose>
                                <c:when test="${enrollment.status == 'ACTIVE'}">
                                    <span class="badge badge-primary">Active</span>
                                </c:when>
                                <c:when test="${enrollment.status == 'COMPLETED'}">
                                    <span class="badge badge-success">Completed</span>
                                </c:when>
                                <c:when test="${enrollment.status == 'DROPPED'}">
                                    <span class="badge badge-danger">Dropped</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-secondary">${enrollment.status}</span>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <!-- Progress Bar -->
                        <div class="mb-3">
                            <div class="d-flex justify-content-between small text-muted mb-1">
                                <span>Progress</span>
                                <span>${enrollment.progressPercent}%</span>
                            </div>
                            <div class="progress" style="height: 8px;">
                                <div class="progress-bar
                                    ${enrollment.progressPercent == 100 ? 'bg-success' : 'bg-primary'}"
                                     style="width: ${enrollment.progressPercent}%"></div>
                            </div>
                        </div>

                        <p class="text-muted small mb-2">
                            <i class="fas fa-calendar-alt"></i> Enrolled:
                            <fmt:formatDate value="${enrollment.enrolledDate}" pattern="MMM dd, yyyy" />
                        </p>

                        <c:if test="${enrollment.status == 'COMPLETED' && not empty enrollment.completionDate}">
                            <p class="text-success small mb-2">
                                <i class="fas fa-check-circle"></i> Completed:
                                <fmt:formatDate value="${enrollment.completionDate}" pattern="MMM dd, yyyy" />
                            </p>
                        </c:if>
                    </div>
                    <div class="card-footer">
                        <div class="d-flex justify-content-between">
                            <a href="<c:url value='/student/course/${enrollment.courseId}' />"
                               class="btn btn-sm btn-primary">
                                <i class="fas fa-play"></i> Continue Learning
                            </a>
                            <c:if test="${enrollment.status == 'COMPLETED'}">
                                <a href="<c:url value='/certificate/view/${enrollment.id}' />"
                                   class="btn btn-sm btn-outline-success">
                                    <i class="fas fa-certificate"></i> Certificate
                                </a>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</div>
