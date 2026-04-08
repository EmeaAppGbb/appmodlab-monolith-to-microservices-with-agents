<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/student/enrollments' />">My Courses</a></li>
            <li class="breadcrumb-item active">Assessment Results</li>
        </ol>
    </nav>

    <c:if test="${not empty success}">
        <div class="alert alert-success">
            <i class="fas fa-check-circle"></i> ${success}
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-triangle"></i> ${error}
        </div>
    </c:if>

    <!-- Score Summary Card -->
    <div class="card shadow mb-4">
        <div class="card-body text-center py-5">
            <c:set var="assessment" value="${assessments[0]}" />

            <div class="mb-3">
                <c:choose>
                    <c:when test="${not empty score && score >= assessment.passingScore}">
                        <div class="text-success">
                            <i class="fas fa-trophy fa-4x mb-3"></i>
                            <h2>Congratulations! You Passed!</h2>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="text-danger">
                            <i class="fas fa-times-circle fa-4x mb-3"></i>
                            <h2>Not Passed</h2>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <h3 class="mb-1">${assessment.title}</h3>
            <p class="text-muted">${assessment.type}</p>

            <!-- Score Display -->
            <div class="row justify-content-center mt-4">
                <div class="col-md-3">
                    <div class="card ${not empty score && score >= assessment.passingScore ? 'border-success' : 'border-danger'}">
                        <div class="card-body">
                            <h5 class="text-muted">Your Score</h5>
                            <h1 class="${not empty score && score >= assessment.passingScore ? 'text-success' : 'text-danger'}">
                                <c:choose>
                                    <c:when test="${not empty score}">${score}%</c:when>
                                    <c:otherwise>--</c:otherwise>
                                </c:choose>
                            </h1>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="text-muted">Passing Score</h5>
                            <h1 class="text-primary">${assessment.passingScore}%</h1>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="text-muted">Status</h5>
                            <h3 class="mt-2">
                                <c:choose>
                                    <c:when test="${not empty score && score >= assessment.passingScore}">
                                        <span class="badge badge-success badge-pill px-3 py-2">PASSED</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-danger badge-pill px-3 py-2">FAILED</span>
                                    </c:otherwise>
                                </c:choose>
                            </h3>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Answers Review -->
    <div class="card shadow mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="fas fa-list-ol"></i> Answer Review</h5>
        </div>
        <div class="card-body" id="answersReview">
            <!-- Rendered from questionsJson via JavaScript -->
        </div>
    </div>

    <!-- Feedback Section -->
    <c:if test="${not empty feedback}">
        <div class="card shadow mb-4">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-comment-dots"></i> Instructor Feedback</h5>
            </div>
            <div class="card-body">
                <p class="mb-0"><c:out value="${feedback}" /></p>
            </div>
        </div>
    </c:if>

    <!-- Actions -->
    <div class="text-center mb-4">
        <c:choose>
            <c:when test="${not empty score && score >= assessment.passingScore}">
                <a href="<c:url value='/student/enrollments' />" class="btn btn-success btn-lg">
                    <i class="fas fa-arrow-right"></i> Continue Learning
                </a>
            </c:when>
            <c:otherwise>
                <a href="<c:url value='/assessment/${assessment.lessonId}' />" class="btn btn-primary btn-lg mr-2">
                    <i class="fas fa-redo"></i> Retry Assessment
                </a>
                <a href="<c:url value='/student/enrollments' />" class="btn btn-outline-secondary btn-lg">
                    <i class="fas fa-arrow-left"></i> Back to Courses
                </a>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<script>
    // Parse and render answer review from assessment questions JSON
    var questionsJson = '${assessment.questionsJson}';
    var questions = [];
    try {
        questions = JSON.parse(questionsJson);
    } catch(e) {
        questions = [];
    }

    var reviewContainer = document.getElementById('answersReview');

    if (questions.length === 0) {
        reviewContainer.innerHTML = '<p class="text-muted text-center">Detailed question review is not available.</p>';
    }

    questions.forEach(function(q, index) {
        var div = document.createElement('div');
        div.className = 'mb-4 pb-3' + (index < questions.length - 1 ? ' border-bottom' : '');

        var header = document.createElement('h6');
        header.innerHTML = '<span class="badge badge-secondary mr-2">Q' + (index + 1) + '</span> ' + q.text;
        div.appendChild(header);

        if (q.correctAnswer) {
            var correct = document.createElement('p');
            correct.className = 'mb-1';
            correct.innerHTML = '<i class="fas fa-check text-success mr-1"></i> <strong>Correct Answer:</strong> ' + q.correctAnswer;
            div.appendChild(correct);
        }

        if (q.options) {
            var optList = document.createElement('ul');
            optList.className = 'list-unstyled ml-4 mt-2';
            q.options.forEach(function(opt) {
                var li = document.createElement('li');
                li.className = 'mb-1';
                if (q.correctAnswer && opt === q.correctAnswer) {
                    li.innerHTML = '<i class="fas fa-check-circle text-success mr-1"></i> ' + opt;
                } else {
                    li.innerHTML = '<i class="far fa-circle text-muted mr-1"></i> ' + opt;
                }
                optList.appendChild(li);
            });
            div.appendChild(optList);
        }

        reviewContainer.appendChild(div);
    });
</script>
