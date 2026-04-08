<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<style>
    .certificate-frame {
        border: 8px double #c9a94e;
        border-radius: 12px;
        background: linear-gradient(135deg, #fdfcfb 0%, #f5f0e8 100%);
        padding: 50px 40px;
        position: relative;
    }
    .certificate-frame::before {
        content: '';
        position: absolute;
        top: 12px; left: 12px; right: 12px; bottom: 12px;
        border: 2px solid #d4af37;
        border-radius: 6px;
        pointer-events: none;
    }
    .certificate-seal {
        width: 100px; height: 100px;
        border-radius: 50%;
        background: linear-gradient(135deg, #d4af37, #c9a94e);
        display: flex; align-items: center; justify-content: center;
        margin: 0 auto;
        box-shadow: 0 4px 12px rgba(201,169,78,0.4);
    }
    .gold-text { color: #c9a94e; }
    .verify-badge { font-family: monospace; letter-spacing: 2px; }
</style>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/student/enrollments' />">My Courses</a></li>
            <li class="breadcrumb-item active">Certificate</li>
        </ol>
    </nav>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <c:if test="${not empty certificate}">
        <!-- Certificate Display -->
        <div class="card shadow-lg mb-4">
            <div class="card-body p-4">
                <div class="certificate-frame text-center" id="certificateContent">
                    <!-- Seal -->
                    <div class="certificate-seal mb-4">
                        <i class="fas fa-award fa-3x text-white"></i>
                    </div>

                    <h6 class="text-uppercase text-muted mb-2" style="letter-spacing: 4px;">Certificate of Completion</h6>
                    <h1 class="gold-text mb-3" style="font-family: 'Georgia', serif;">
                        <i class="fas fa-graduation-cap"></i> EduVerse Academy
                    </h1>

                    <hr class="mx-auto" style="width:60%; border-color:#d4af37;">

                    <p class="text-muted mb-1">This is to certify that</p>
                    <h2 class="mb-3" style="font-family: 'Georgia', serif;">
                        <sec:authorize access="isAuthenticated()">
                            <sec:authentication property="principal.username" />
                        </sec:authorize>
                    </h2>

                    <p class="text-muted mb-1">has successfully completed the course</p>
                    <h3 class="text-primary mb-4">
                        <c:out value="${courseName}" default="Course" />
                    </h3>

                    <div class="row justify-content-center mt-4">
                        <div class="col-md-4 text-center">
                            <p class="mb-0 text-muted small">Date Issued</p>
                            <h5>
                                <fmt:formatDate value="${certificate.issuedDate}" pattern="MMMM dd, yyyy" />
                            </h5>
                        </div>
                        <div class="col-md-4 text-center">
                            <p class="mb-0 text-muted small">Certificate Number</p>
                            <h5 class="verify-badge">${certificate.certificateNumber}</h5>
                        </div>
                    </div>

                    <hr class="mx-auto mt-4" style="width:40%; border-color:#d4af37;">
                    <p class="text-muted small mb-0">
                        <i class="fas fa-shield-alt"></i>
                        Verify at eduverse.academy/verify/${certificate.certificateNumber}
                    </p>
                </div>
            </div>
        </div>

        <!-- Certificate Details & Actions -->
        <div class="row mb-4">
            <div class="col-md-6">
                <div class="card shadow-sm">
                    <div class="card-header bg-white">
                        <h5 class="mb-0"><i class="fas fa-info-circle"></i> Certificate Details</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-borderless mb-0">
                            <tr>
                                <td class="text-muted">Certificate ID:</td>
                                <td><strong>${certificate.id}</strong></td>
                            </tr>
                            <tr>
                                <td class="text-muted">Verification Number:</td>
                                <td><code class="verify-badge">${certificate.certificateNumber}</code></td>
                            </tr>
                            <tr>
                                <td class="text-muted">Enrollment ID:</td>
                                <td>${certificate.enrollmentId}</td>
                            </tr>
                            <tr>
                                <td class="text-muted">Issue Date:</td>
                                <td><fmt:formatDate value="${certificate.issuedDate}" pattern="MMM dd, yyyy" /></td>
                            </tr>
                            <c:if test="${not empty certificate.templateId}">
                                <tr>
                                    <td class="text-muted">Template:</td>
                                    <td>${certificate.templateId}</td>
                                </tr>
                            </c:if>
                        </table>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card shadow-sm">
                    <div class="card-header bg-white">
                        <h5 class="mb-0"><i class="fas fa-download"></i> Actions</h5>
                    </div>
                    <div class="card-body text-center">
                        <p class="text-muted">Download or share your certificate of achievement.</p>
                        <a href="<c:url value='/certificate/download/${certificate.enrollmentId}' />"
                           class="btn btn-primary btn-lg mb-3 btn-block">
                            <i class="fas fa-file-pdf"></i> Download PDF
                        </a>
                        <button class="btn btn-outline-secondary btn-block" onclick="window.print();">
                            <i class="fas fa-print"></i> Print Certificate
                        </button>
                        <hr>
                        <a href="<c:url value='/student/enrollments' />" class="btn btn-outline-primary btn-block">
                            <i class="fas fa-arrow-left"></i> Back to My Courses
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
</div>
