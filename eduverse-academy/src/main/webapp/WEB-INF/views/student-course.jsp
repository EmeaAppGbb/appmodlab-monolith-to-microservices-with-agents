<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<style>
    .lesson-sidebar { max-height: calc(100vh - 120px); overflow-y: auto; }
    .lesson-item { cursor: pointer; transition: background-color 0.2s; }
    .lesson-item:hover { background-color: #f0f0f0; }
    .lesson-item.active { background-color: #e7f1ff; border-left: 3px solid #007bff; }
    .lesson-item.completed { color: #28a745; }
    .video-container { position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; background: #000; }
    .video-container video, .video-container iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
    .progress-ring { width: 60px; height: 60px; }
</style>

<div class="container-fluid mt-3">
    <c:if test="${not empty error}">
        <div class="alert alert-danger">${error}</div>
    </c:if>

    <c:if test="${empty enrollment}">
        <div class="alert alert-warning">
            <i class="fas fa-exclamation-triangle"></i> You are not enrolled in this course.
            <a href="<c:url value='/course/view/${course.id}' />" class="alert-link">View course details</a> to enroll.
        </div>
    </c:if>

    <c:if test="${not empty enrollment}">
        <div class="row">
            <!-- Sidebar: Module/Lesson Navigation -->
            <div class="col-lg-3 col-md-4 mb-3">
                <div class="card shadow-sm lesson-sidebar">
                    <div class="card-header bg-dark text-white">
                        <h6 class="mb-0">
                            <i class="fas fa-book"></i> <c:out value="${course.title}" />
                        </h6>
                    </div>

                    <!-- Progress Summary -->
                    <div class="card-body py-2 border-bottom">
                        <div class="d-flex align-items-center justify-content-between">
                            <small class="text-muted">Your Progress</small>
                            <strong class="text-primary">${enrollment.progressPercent}%</strong>
                        </div>
                        <div class="progress mt-1" style="height: 6px;">
                            <div class="progress-bar ${enrollment.progressPercent == 100 ? 'bg-success' : 'bg-primary'}"
                                 style="width: ${enrollment.progressPercent}%"></div>
                        </div>
                    </div>

                    <!-- Module/Lesson Tree -->
                    <div class="list-group list-group-flush">
                        <c:set var="lessonIndex" value="0" />
                        <c:forEach var="module" items="${course.modules}" varStatus="ms">
                            <div class="list-group-item bg-light py-2">
                                <strong class="small text-uppercase text-muted">
                                    Module ${module.sortOrder}: ${module.title}
                                </strong>
                            </div>
                            <c:forEach var="lesson" items="${module.lessons}" varStatus="ls">
                                <a href="#" class="list-group-item list-group-item-action lesson-item py-2 ${lessonIndex == 0 ? 'active' : ''}"
                                   data-lesson-index="${lessonIndex}"
                                   data-lesson-title="${lesson.title}"
                                   data-lesson-type="${lesson.contentType}"
                                   data-lesson-video="${lesson.videoUrl}"
                                   data-lesson-content="${lesson.content}"
                                   data-lesson-duration="${lesson.durationMinutes}"
                                   data-lesson-id="${lesson.id}">
                                    <div class="d-flex align-items-center">
                                        <c:choose>
                                            <c:when test="${lesson.contentType == 'VIDEO'}">
                                                <i class="fas fa-play-circle text-primary mr-2"></i>
                                            </c:when>
                                            <c:when test="${lesson.contentType == 'QUIZ'}">
                                                <i class="fas fa-question-circle text-warning mr-2"></i>
                                            </c:when>
                                            <c:when test="${lesson.contentType == 'DOCUMENT'}">
                                                <i class="fas fa-file-alt text-secondary mr-2"></i>
                                            </c:when>
                                            <c:otherwise>
                                                <i class="fas fa-tasks text-info mr-2"></i>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="flex-grow-1">
                                            <span class="small">${lesson.title}</span><br>
                                            <span class="text-muted" style="font-size:0.7rem;">${lesson.durationMinutes} min</span>
                                        </div>
                                    </div>
                                </a>
                                <c:set var="lessonIndex" value="${lessonIndex + 1}" />
                            </c:forEach>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <!-- Main Content Area -->
            <div class="col-lg-9 col-md-8">
                <!-- Video Player Area -->
                <div class="card shadow-sm mb-3">
                    <div class="video-container" id="videoPlayerArea">
                        <div class="d-flex align-items-center justify-content-center h-100 text-white" id="videoPlaceholder">
                            <div class="text-center">
                                <i class="fas fa-play-circle fa-4x mb-3"></i>
                                <p>Select a lesson to start learning</p>
                            </div>
                        </div>
                        <video id="videoPlayer" controls style="display:none;">
                            <source src="" type="video/mp4">
                            Your browser does not support the video tag.
                        </video>
                    </div>
                </div>

                <!-- Lesson Info -->
                <div class="card shadow-sm mb-3">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <h4 id="lessonTitle">
                                    <c:choose>
                                        <c:when test="${not empty course.modules && not empty course.modules[0].lessons}">
                                            ${course.modules[0].lessons[0].title}
                                        </c:when>
                                        <c:otherwise>No lessons available</c:otherwise>
                                    </c:choose>
                                </h4>
                                <span class="badge badge-info" id="lessonTypeBadge">VIDEO</span>
                                <span class="text-muted small ml-2" id="lessonDuration"></span>
                            </div>
                            <div>
                                <button class="btn btn-sm btn-outline-success" id="markCompleteBtn"
                                        onclick="markLessonComplete()">
                                    <i class="fas fa-check"></i> Mark Complete
                                </button>
                            </div>
                        </div>

                        <!-- Lesson Content -->
                        <hr>
                        <div id="lessonContent" class="mt-3">
                            <c:if test="${not empty course.modules && not empty course.modules[0].lessons}">
                                <c:out value="${course.modules[0].lessons[0].content}" escapeXml="false" />
                            </c:if>
                        </div>

                        <!-- Assessment Link -->
                        <div id="quizSection" style="display:none;" class="mt-3">
                            <div class="alert alert-warning">
                                <i class="fas fa-question-circle"></i> This lesson has a quiz.
                                <a href="#" id="quizLink" class="alert-link">Take the Assessment</a>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Navigation Buttons -->
                <div class="d-flex justify-content-between mb-4">
                    <button class="btn btn-outline-primary" id="prevLessonBtn" onclick="navigateLesson(-1)" disabled>
                        <i class="fas fa-chevron-left"></i> Previous Lesson
                    </button>
                    <a href="<c:url value='/student/enrollments' />" class="btn btn-outline-secondary">
                        <i class="fas fa-th-list"></i> All Courses
                    </a>
                    <button class="btn btn-primary" id="nextLessonBtn" onclick="navigateLesson(1)">
                        Next Lesson <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        </div>
    </c:if>
</div>

<script>
    var currentLessonIndex = 0;
    var lessonItems = document.querySelectorAll('.lesson-item');
    var totalLessons = lessonItems.length;

    // Click handler for sidebar lessons
    document.querySelectorAll('.lesson-item').forEach(function(item) {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            var idx = parseInt(this.getAttribute('data-lesson-index'));
            selectLesson(idx);
        });
    });

    function selectLesson(index) {
        if (index < 0 || index >= totalLessons) return;
        currentLessonIndex = index;
        var item = lessonItems[index];

        // Update sidebar active state
        document.querySelectorAll('.lesson-item').forEach(function(el) { el.classList.remove('active'); });
        item.classList.add('active');

        // Update lesson info
        document.getElementById('lessonTitle').textContent = item.getAttribute('data-lesson-title');
        document.getElementById('lessonTypeBadge').textContent = item.getAttribute('data-lesson-type');
        document.getElementById('lessonDuration').textContent = item.getAttribute('data-lesson-duration') + ' min';
        document.getElementById('lessonContent').textContent = item.getAttribute('data-lesson-content') || '';

        // Video handling
        var videoUrl = item.getAttribute('data-lesson-video');
        var videoPlayer = document.getElementById('videoPlayer');
        var placeholder = document.getElementById('videoPlaceholder');
        if (videoUrl && item.getAttribute('data-lesson-type') === 'VIDEO') {
            videoPlayer.querySelector('source').src = videoUrl;
            videoPlayer.load();
            videoPlayer.style.display = 'block';
            placeholder.style.display = 'none';
        } else {
            videoPlayer.style.display = 'none';
            placeholder.style.display = 'flex';
        }

        // Quiz link
        var quizSection = document.getElementById('quizSection');
        if (item.getAttribute('data-lesson-type') === 'QUIZ') {
            quizSection.style.display = 'block';
            document.getElementById('quizLink').href = '<c:url value="/assessment/" />' + item.getAttribute('data-lesson-id');
        } else {
            quizSection.style.display = 'none';
        }

        // Update nav buttons
        document.getElementById('prevLessonBtn').disabled = (index === 0);
        document.getElementById('nextLessonBtn').disabled = (index === totalLessons - 1);
    }

    function navigateLesson(direction) {
        selectLesson(currentLessonIndex + direction);
    }

    function markLessonComplete() {
        var btn = document.getElementById('markCompleteBtn');
        btn.innerHTML = '<i class="fas fa-check-circle"></i> Completed';
        btn.classList.remove('btn-outline-success');
        btn.classList.add('btn-success');
        btn.disabled = true;
        lessonItems[currentLessonIndex].classList.add('completed');
    }

    // Initialize first lesson
    if (totalLessons > 0) {
        selectLesson(0);
    }
</script>
