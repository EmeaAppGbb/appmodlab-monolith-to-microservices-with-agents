<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="container mt-4">
    <h2 class="mb-4"><i class="fas fa-tachometer-alt"></i> Admin Dashboard</h2>

    <!-- Platform Statistics -->
    <div class="row mb-4">
        <div class="col-md-3">
            <div class="card bg-primary text-white shadow">
                <div class="card-body text-center">
                    <i class="fas fa-book fa-2x mb-2"></i>
                    <h3>${totalCourses}</h3>
                    <p class="mb-0">Total Courses</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card bg-success text-white shadow">
                <div class="card-body text-center">
                    <i class="fas fa-users fa-2x mb-2"></i>
                    <h3>${totalUsers}</h3>
                    <p class="mb-0">Total Users</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card bg-info text-white shadow">
                <div class="card-body text-center">
                    <i class="fas fa-user-graduate fa-2x mb-2"></i>
                    <h3>${totalEnrollments}</h3>
                    <p class="mb-0">Total Enrollments</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card bg-warning text-dark shadow">
                <div class="card-body text-center">
                    <i class="fas fa-spinner fa-2x mb-2"></i>
                    <h3>${activeEnrollments}</h3>
                    <p class="mb-0">Active Enrollments</p>
                </div>
            </div>
        </div>
    </div>

    <!-- Management Links -->
    <div class="row">
        <div class="col-md-6 mb-4">
            <div class="card shadow">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-book"></i> Course Management</h5>
                </div>
                <div class="card-body">
                    <p class="text-muted">Manage all courses, review content, and moderate publications.</p>
                    <a href="<c:url value='/admin/courses' />" class="btn btn-primary">
                        <i class="fas fa-list"></i> Manage Courses
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-6 mb-4">
            <div class="card shadow">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-users-cog"></i> User Management</h5>
                </div>
                <div class="card-body">
                    <p class="text-muted">Manage user accounts, roles, and permissions across the platform.</p>
                    <a href="<c:url value='/admin/users' />" class="btn btn-primary">
                        <i class="fas fa-user-edit"></i> Manage Users
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-6 mb-4">
            <div class="card shadow">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-chart-line"></i> Reports</h5>
                </div>
                <div class="card-body">
                    <p class="text-muted">View enrollment trends, revenue reports, and platform analytics.</p>
                    <a href="#" class="btn btn-outline-primary">
                        <i class="fas fa-file-alt"></i> View Reports
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-6 mb-4">
            <div class="card shadow">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-cog"></i> Settings</h5>
                </div>
                <div class="card-body">
                    <p class="text-muted">Configure platform settings, payment gateways, and notifications.</p>
                    <a href="#" class="btn btn-outline-primary">
                        <i class="fas fa-wrench"></i> Platform Settings
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
