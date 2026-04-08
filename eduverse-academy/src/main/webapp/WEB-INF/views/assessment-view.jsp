<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<style>
    .timer-bar { position: sticky; top: 56px; z-index: 100; }
    .timer-warning { animation: pulse 1s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
    .question-card { transition: border-color 0.2s; }
    .question-card:focus-within { border-color: #007bff; }
</style>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/student/enrollments' />">My Courses</a></li>
            <li class="breadcrumb-item active">${assessment.title}</li>
        </ol>
    </nav>

    <!-- Timer Bar -->
    <c:if test="${assessment.timeLimitMinutes > 0}">
        <div class="alert alert-info timer-bar d-flex justify-content-between align-items-center mb-4" id="timerBar">
            <div>
                <i class="fas fa-clock"></i> <strong>Time Remaining:</strong>
                <span id="timerDisplay" class="h5 mb-0 ml-2">${assessment.timeLimitMinutes}:00</span>
            </div>
            <div>
                <span class="badge badge-pill badge-info">${assessment.type}</span>
                <span class="ml-2 text-muted small">Passing score: ${assessment.passingScore}%</span>
            </div>
        </div>
    </c:if>

    <!-- Assessment Header -->
    <div class="card shadow-sm mb-4">
        <div class="card-body">
            <h3><i class="fas fa-clipboard-list"></i> ${assessment.title}</h3>
            <p class="text-muted mb-0">
                <span class="badge badge-primary mr-2">${assessment.type}</span>
                <c:if test="${assessment.timeLimitMinutes > 0}">
                    <i class="fas fa-hourglass-half"></i> ${assessment.timeLimitMinutes} minutes
                    <span class="mx-2">|</span>
                </c:if>
                <i class="fas fa-check-circle"></i> Passing Score: ${assessment.passingScore}%
            </p>
        </div>
    </div>

    <!-- Assessment Form -->
    <form action="<c:url value='/assessment/submit' />" method="post" id="assessmentForm"
          onsubmit="return confirmSubmit();">
        <input type="hidden" name="assessmentId" value="${assessment.id}" />
        <input type="hidden" name="answersJson" id="answersJsonField" />

        <!-- Questions rendered from questionsJson -->
        <div id="questionsContainer">
            <!-- Questions are populated from assessment.questionsJson via JavaScript -->
        </div>

        <!-- Submit Area -->
        <div class="card shadow-sm mb-4">
            <div class="card-body text-center">
                <p class="text-muted mb-3">
                    <i class="fas fa-info-circle"></i> Review your answers before submitting.
                    Once submitted, you cannot change your responses.
                </p>
                <button type="submit" class="btn btn-primary btn-lg" id="submitBtn">
                    <i class="fas fa-paper-plane"></i> Submit Assessment
                </button>
            </div>
        </div>
    </form>
</div>

<script>
    var timeLimit = ${assessment.timeLimitMinutes > 0 ? assessment.timeLimitMinutes : 0};
    var totalSeconds = timeLimit * 60;
    var timerInterval = null;

    // Parse and render questions from JSON
    var questionsJson = '${assessment.questionsJson}';
    var questions = [];
    try {
        questions = JSON.parse(questionsJson);
    } catch(e) {
        // Fallback sample if JSON parsing fails
        questions = [
            { id: 1, text: "Sample question — assessment questions could not be loaded.", type: "multiple_choice", options: ["Option A", "Option B", "Option C", "Option D"] }
        ];
    }

    var container = document.getElementById('questionsContainer');

    questions.forEach(function(q, index) {
        var card = document.createElement('div');
        card.className = 'card shadow-sm mb-3 question-card';
        var body = document.createElement('div');
        body.className = 'card-body';

        // Question text
        var qText = document.createElement('h5');
        qText.innerHTML = '<span class="badge badge-secondary mr-2">Q' + (index + 1) + '</span> ' + q.text;
        body.appendChild(qText);

        if (q.type === 'multiple_choice' && q.options) {
            // Radio button options
            q.options.forEach(function(opt, optIdx) {
                var div = document.createElement('div');
                div.className = 'custom-control custom-radio my-2 ml-3';

                var input = document.createElement('input');
                input.type = 'radio';
                input.className = 'custom-control-input';
                input.id = 'q' + q.id + '_opt' + optIdx;
                input.name = 'question_' + q.id;
                input.value = opt;

                var label = document.createElement('label');
                label.className = 'custom-control-label';
                label.htmlFor = 'q' + q.id + '_opt' + optIdx;
                label.textContent = opt;

                div.appendChild(input);
                div.appendChild(label);
                body.appendChild(div);
            });
        } else {
            // Text input for open-ended questions
            var textDiv = document.createElement('div');
            textDiv.className = 'form-group mt-3';
            var textarea = document.createElement('textarea');
            textarea.className = 'form-control';
            textarea.name = 'question_' + q.id;
            textarea.rows = 3;
            textarea.placeholder = 'Type your answer here...';
            textDiv.appendChild(textarea);
            body.appendChild(textDiv);
        }

        card.appendChild(body);
        container.appendChild(card);
    });

    // Collect answers into JSON before submit
    function collectAnswers() {
        var answers = {};
        questions.forEach(function(q) {
            if (q.type === 'multiple_choice') {
                var selected = document.querySelector('input[name="question_' + q.id + '"]:checked');
                answers[q.id] = selected ? selected.value : null;
            } else {
                var textarea = document.querySelector('textarea[name="question_' + q.id + '"]');
                answers[q.id] = textarea ? textarea.value : '';
            }
        });
        return JSON.stringify(answers);
    }

    function confirmSubmit() {
        var unanswered = 0;
        questions.forEach(function(q) {
            if (q.type === 'multiple_choice') {
                if (!document.querySelector('input[name="question_' + q.id + '"]:checked')) unanswered++;
            } else {
                var ta = document.querySelector('textarea[name="question_' + q.id + '"]');
                if (!ta || !ta.value.trim()) unanswered++;
            }
        });

        var msg = 'Are you sure you want to submit?';
        if (unanswered > 0) {
            msg = 'You have ' + unanswered + ' unanswered question(s). Submit anyway?';
        }
        if (confirm(msg)) {
            document.getElementById('answersJsonField').value = collectAnswers();
            return true;
        }
        return false;
    }

    // Timer countdown
    if (timeLimit > 0) {
        timerInterval = setInterval(function() {
            totalSeconds--;
            if (totalSeconds <= 0) {
                clearInterval(timerInterval);
                document.getElementById('answersJsonField').value = collectAnswers();
                document.getElementById('assessmentForm').submit();
                return;
            }
            var mins = Math.floor(totalSeconds / 60);
            var secs = totalSeconds % 60;
            var display = document.getElementById('timerDisplay');
            display.textContent = mins + ':' + (secs < 10 ? '0' : '') + secs;

            // Warning when under 2 minutes
            if (totalSeconds <= 120) {
                document.getElementById('timerBar').classList.remove('alert-info');
                document.getElementById('timerBar').classList.add('alert-danger');
                display.classList.add('timer-warning');
            }
        }, 1000);
    }
</script>
