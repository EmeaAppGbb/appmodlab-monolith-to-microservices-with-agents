<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-8 text-center">
            <div class="card shadow-sm border-0">
                <div class="card-body py-5">
                    <div class="text-danger mb-4">
                        <i class="fas fa-exclamation-triangle fa-5x"></i>
                    </div>

                    <h2 class="mb-3">Oops! Something went wrong.</h2>

                    <c:choose>
                        <c:when test="${not empty error}">
                            <div class="alert alert-danger d-inline-block">
                                <i class="fas fa-info-circle"></i> <c:out value="${error}" />
                            </div>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted lead">
                                An unexpected error occurred. Please try again later.
                            </p>
                        </c:otherwise>
                    </c:choose>

                    <div class="mt-4">
                        <a href="<c:url value='/home' />" class="btn btn-primary btn-lg mr-2">
                            <i class="fas fa-home"></i> Back to Home
                        </a>
                        <a href="javascript:history.back();" class="btn btn-outline-secondary btn-lg">
                            <i class="fas fa-arrow-left"></i> Go Back
                        </a>
                    </div>

                    <hr class="my-4">

                    <p class="text-muted small mb-0">
                        If this problem persists, please <a href="<c:url value='/contact' />">contact support</a>.
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>
