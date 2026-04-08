<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/catalog' />">Catalog</a></li>
            <li class="breadcrumb-item active">${course.title}</li>
        </ol>
    </nav>

    <div class="row">
        <!-- Course Info -->
        <div class="col-md-8">
            <h2>${course.title}</h2>
            <p class="text-muted">
                <span class="badge badge-info">${course.category}</span>
                <span class="ml-2"><i class="fas fa-clock"></i> ${course.durationHours} hours</span>
                <span class="ml-2"><i class="fas fa-signal"></i> ${course.status}</span>
            </p>
            <p class="lead"><c:out value="${course.description}" /></p>

            <hr>

            <!-- Modules & Lessons -->
            <h4 class="mb-3">Course Content</h4>
            <c:if test="${empty course.modules}">
                <p class="text-muted">No modules have been added to this course yet.</p>
            </c:if>
            <div class="accordion" id="moduleAccordion">
                <c:forEach var="module" items="${course.modules}" varStatus="ms">
                    <div class="card mb-2">
                        <div class="card-header" id="heading${ms.index}">
                            <h5 class="mb-0">
                                <button class="btn btn-link text-left w-100 ${ms.index > 0 ? 'collapsed' : ''}"
                                        data-toggle="collapse" data-target="#collapse${ms.index}">
                                    <i class="fas fa-folder mr-2"></i>
                                    Module ${module.sortOrder}: ${module.title}
                                    <span class="badge badge-pill badge-light float-right">
                                        ${module.lessons.size()} lessons
                                    </span>
                                </button>
                            </h5>
                        </div>
                        <div id="collapse${ms.index}" class="collapse ${ms.index == 0 ? 'show' : ''}"
                             data-parent="#moduleAccordion">
                            <ul class="list-group list-group-flush">
                                <c:forEach var="lesson" items="${module.lessons}">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>
                                            <c:choose>
                                                <c:when test="${lesson.contentType == 'VIDEO'}">
                                                    <i class="fas fa-play-circle text-primary"></i>
                                                </c:when>
                                                <c:when test="${lesson.contentType == 'QUIZ'}">
                                                    <i class="fas fa-question-circle text-warning"></i>
                                                </c:when>
                                                <c:when test="${lesson.contentType == 'DOCUMENT'}">
                                                    <i class="fas fa-file-alt text-secondary"></i>
                                                </c:when>
                                                <c:otherwise>
                                                    <i class="fas fa-tasks text-info"></i>
                                                </c:otherwise>
                                            </c:choose>
                                            ${lesson.title}
                                        </span>
                                        <span class="text-muted small">${lesson.durationMinutes} min</span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- Sidebar -->
        <div class="col-md-4">
            <div class="card shadow sticky-top" style="top:80px;">
                <c:choose>
                    <c:when test="${not empty course.thumbnailUrl}">
                        <img src="${course.thumbnailUrl}" class="card-img-top" alt="${course.title}">
                    </c:when>
                    <c:otherwise>
                        <div class="bg-primary text-white d-flex align-items-center justify-content-center" style="height:200px;">
                            <i class="fas fa-book-open fa-4x"></i>
                        </div>
                    </c:otherwise>
                </c:choose>
                <div class="card-body text-center">
                    <h3 class="text-primary">
                        $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                    </h3>
                    <sec:authorize access="isAuthenticated()">
                        <form action="<c:url value='/enroll/${course.id}' />" method="post">
                            <button type="submit" class="btn btn-success btn-lg btn-block mt-3">
                                <i class="fas fa-user-plus"></i> Enroll Now
                            </button>
                        </form>
                    </sec:authorize>
                    <sec:authorize access="isAnonymous()">
                        <a href="<c:url value='/login' />" class="btn btn-outline-primary btn-lg btn-block mt-3">
                            Sign in to Enroll
                        </a>
                    </sec:authorize>
                    <hr>
                    <ul class="list-unstyled text-left">
                        <li class="mb-2"><i class="fas fa-clock text-muted mr-2"></i> ${course.durationHours} hours of content</li>
                        <li class="mb-2"><i class="fas fa-layer-group text-muted mr-2"></i> ${course.modules.size()} modules</li>
                        <li class="mb-2"><i class="fas fa-certificate text-muted mr-2"></i> Certificate of completion</li>
                        <li class="mb-2"><i class="fas fa-infinity text-muted mr-2"></i> Lifetime access</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</div>
