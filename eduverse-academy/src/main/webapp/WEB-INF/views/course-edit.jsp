<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/instructor/dashboard' />">Dashboard</a></li>
            <li class="breadcrumb-item active">
                <c:choose>
                    <c:when test="${not empty course.id}">Edit Course</c:when>
                    <c:otherwise>Create Course</c:otherwise>
                </c:choose>
            </li>
        </ol>
    </nav>

    <h2 class="mb-4">
        <i class="fas fa-edit"></i>
        <c:choose>
            <c:when test="${not empty course.id}">Edit Course</c:when>
            <c:otherwise>Create New Course</c:otherwise>
        </c:choose>
    </h2>

    <c:if test="${not empty success}">
        <div class="alert alert-success alert-dismissible fade show">
            <i class="fas fa-check-circle"></i> ${success}
            <button type="button" class="close" data-dismiss="alert">&times;</button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show">
            <i class="fas fa-exclamation-triangle"></i> ${error}
            <button type="button" class="close" data-dismiss="alert">&times;</button>
        </div>
    </c:if>

    <!-- Course Details Form -->
    <form action="<c:url value='/course/create' />" method="post" id="courseForm">
        <c:if test="${not empty course.id}">
            <input type="hidden" name="id" value="${course.id}" />
        </c:if>

        <div class="card shadow mb-4">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="fas fa-info-circle"></i> Course Details</h5>
            </div>
            <div class="card-body">
                <div class="form-group">
                    <label for="title">Course Title <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" id="title" name="title"
                           value="<c:out value='${course.title}' />" required
                           placeholder="e.g. Introduction to Machine Learning">
                </div>

                <div class="form-group">
                    <label for="description">Description <span class="text-danger">*</span></label>
                    <textarea class="form-control" id="description" name="description" rows="4"
                              required placeholder="Describe what students will learn..."><c:out value="${course.description}" /></textarea>
                </div>

                <div class="form-row">
                    <div class="form-group col-md-4">
                        <label for="category">Category <span class="text-danger">*</span></label>
                        <select class="form-control" id="category" name="category" required>
                            <option value="">Select a category</option>
                            <c:forEach var="cat" items="${['Programming','Data Science','Web Development','Mobile Development','DevOps','Cloud Computing','Design','Business']}">
                                <option value="${cat}" ${course.category == cat ? 'selected' : ''}>${cat}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group col-md-4">
                        <label for="price">Price (USD) <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text">$</span>
                            </div>
                            <input type="number" class="form-control" id="price" name="price"
                                   step="0.01" min="0" value="${course.price}" required
                                   placeholder="49.99">
                        </div>
                    </div>
                    <div class="form-group col-md-4">
                        <label for="durationHours">Duration (hours)</label>
                        <input type="number" class="form-control" id="durationHours" name="durationHours"
                               min="0" value="${course.durationHours}" placeholder="10">
                    </div>
                </div>

                <div class="form-group">
                    <label for="thumbnailUrl">Thumbnail URL</label>
                    <input type="url" class="form-control" id="thumbnailUrl" name="thumbnailUrl"
                           value="<c:out value='${course.thumbnailUrl}' />"
                           placeholder="https://example.com/image.jpg">
                    <small class="form-text text-muted">Paste a URL to an image for the course card.</small>
                </div>

                <c:if test="${not empty course.thumbnailUrl}">
                    <div class="mb-3">
                        <label>Preview:</label><br>
                        <img src="${course.thumbnailUrl}" alt="Thumbnail preview"
                             class="img-thumbnail" style="max-height:150px;">
                    </div>
                </c:if>

                <div class="d-flex justify-content-between align-items-center">
                    <c:if test="${not empty course.id}">
                        <span class="text-muted small">
                            Status: <span class="badge ${course.status == 'PUBLISHED' ? 'badge-success' : 'badge-warning'}">
                                ${course.status}
                            </span>
                        </span>
                    </c:if>
                    <div class="ml-auto">
                        <a href="<c:url value='/instructor/dashboard' />" class="btn btn-outline-secondary mr-2">
                            <i class="fas fa-times"></i> Cancel
                        </a>
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-save"></i>
                            <c:choose>
                                <c:when test="${not empty course.id}">Save Changes</c:when>
                                <c:otherwise>Create Course</c:otherwise>
                            </c:choose>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </form>

    <!-- Modules & Lessons (only shown for existing courses) -->
    <c:if test="${not empty course.id}">
        <div class="card shadow mb-4">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 class="mb-0"><i class="fas fa-layer-group"></i> Modules &amp; Lessons</h5>
                <button type="button" class="btn btn-sm btn-success" data-toggle="modal" data-target="#addModuleModal">
                    <i class="fas fa-plus"></i> Add Module
                </button>
            </div>
            <div class="card-body">
                <c:if test="${empty course.modules}">
                    <div class="text-center text-muted py-4">
                        <i class="fas fa-folder-open fa-3x mb-3 d-block"></i>
                        No modules yet. Click "Add Module" to build your course content.
                    </div>
                </c:if>

                <div id="moduleList">
                    <c:forEach var="module" items="${course.modules}" varStatus="ms">
                        <div class="card mb-3 border-left-primary" data-module-id="${module.id}">
                            <div class="card-header bg-light d-flex justify-content-between align-items-center">
                                <div>
                                    <i class="fas fa-grip-vertical text-muted mr-2" style="cursor:grab;" title="Drag to reorder"></i>
                                    <strong>Module ${module.sortOrder}: ${module.title}</strong>
                                </div>
                                <div>
                                    <button type="button" class="btn btn-sm btn-outline-primary"
                                            title="Add Lesson" data-toggle="modal" data-target="#addLessonModal"
                                            data-module-id="${module.id}">
                                        <i class="fas fa-plus"></i> Lesson
                                    </button>
                                    <button type="button" class="btn btn-sm btn-outline-danger"
                                            title="Remove Module"
                                            onclick="if(confirm('Remove this module and all its lessons?')) { document.getElementById('deleteModuleForm_${module.id}').submit(); }">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                    <form id="deleteModuleForm_${module.id}"
                                          action="<c:url value='/course/${course.id}/module/${module.id}/delete' />"
                                          method="post" class="d-none"></form>
                                </div>
                            </div>
                            <c:if test="${not empty module.description}">
                                <div class="card-body pb-2 pt-2">
                                    <p class="text-muted small mb-0">${module.description}</p>
                                </div>
                            </c:if>

                            <!-- Lessons list -->
                            <ul class="list-group list-group-flush">
                                <c:if test="${empty module.lessons}">
                                    <li class="list-group-item text-muted small text-center">
                                        No lessons in this module yet.
                                    </li>
                                </c:if>
                                <c:forEach var="lesson" items="${module.lessons}">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <div>
                                            <i class="fas fa-grip-vertical text-muted mr-2" style="cursor:grab;"></i>
                                            <c:choose>
                                                <c:when test="${lesson.contentType == 'VIDEO'}">
                                                    <i class="fas fa-play-circle text-primary mr-1"></i>
                                                </c:when>
                                                <c:when test="${lesson.contentType == 'QUIZ'}">
                                                    <i class="fas fa-question-circle text-warning mr-1"></i>
                                                </c:when>
                                                <c:when test="${lesson.contentType == 'DOCUMENT'}">
                                                    <i class="fas fa-file-alt text-secondary mr-1"></i>
                                                </c:when>
                                                <c:otherwise>
                                                    <i class="fas fa-tasks text-info mr-1"></i>
                                                </c:otherwise>
                                            </c:choose>
                                            ${lesson.title}
                                            <span class="badge badge-light ml-2">${lesson.durationMinutes} min</span>
                                        </div>
                                        <div>
                                            <button type="button" class="btn btn-sm btn-outline-danger"
                                                    onclick="if(confirm('Remove this lesson?')) { document.getElementById('deleteLessonForm_${lesson.id}').submit(); }">
                                                <i class="fas fa-times"></i>
                                            </button>
                                            <form id="deleteLessonForm_${lesson.id}"
                                                  action="<c:url value='/course/${course.id}/lesson/${lesson.id}/delete' />"
                                                  method="post" class="d-none"></form>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </c:if>
</div>

<!-- Add Module Modal -->
<c:if test="${not empty course.id}">
    <div class="modal fade" id="addModuleModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<c:url value='/course/${course.id}/module/add' />" method="post">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="fas fa-folder-plus"></i> Add Module</h5>
                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label for="moduleTitle">Module Title <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="moduleTitle" name="title" required
                                   placeholder="e.g. Getting Started">
                        </div>
                        <div class="form-group">
                            <label for="moduleDescription">Description</label>
                            <textarea class="form-control" id="moduleDescription" name="description"
                                      rows="2" placeholder="Brief module overview..."></textarea>
                        </div>
                        <div class="form-group">
                            <label for="moduleSortOrder">Sort Order</label>
                            <input type="number" class="form-control" id="moduleSortOrder" name="sortOrder"
                                   min="1" value="${course.modules.size() + 1}">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-success"><i class="fas fa-plus"></i> Add Module</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Add Lesson Modal -->
    <div class="modal fade" id="addLessonModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<c:url value='/course/${course.id}/lesson/add' />" method="post">
                    <input type="hidden" name="moduleId" id="lessonModuleId" />
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="fas fa-file-medical"></i> Add Lesson</h5>
                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label for="lessonTitle">Lesson Title <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="lessonTitle" name="title" required
                                   placeholder="e.g. Introduction Video">
                        </div>
                        <div class="form-group">
                            <label for="lessonContentType">Content Type</label>
                            <select class="form-control" id="lessonContentType" name="contentType">
                                <option value="VIDEO">Video</option>
                                <option value="DOCUMENT">Document</option>
                                <option value="QUIZ">Quiz</option>
                                <option value="ASSIGNMENT">Assignment</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="lessonVideoUrl">Video URL</label>
                            <input type="url" class="form-control" id="lessonVideoUrl" name="videoUrl"
                                   placeholder="https://example.com/video.mp4">
                        </div>
                        <div class="form-group">
                            <label for="lessonDuration">Duration (minutes)</label>
                            <input type="number" class="form-control" id="lessonDuration" name="durationMinutes"
                                   min="0" placeholder="15">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-success"><i class="fas fa-plus"></i> Add Lesson</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<script>
    // Pass moduleId to the Add Lesson modal
    $('#addLessonModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var moduleId = button.data('module-id');
        $(this).find('#lessonModuleId').val(moduleId);
    });
</script>
