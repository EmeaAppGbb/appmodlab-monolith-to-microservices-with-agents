<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-5">
            <div class="card shadow">
                <div class="card-body p-4">
                    <h3 class="card-title text-center mb-4">
                        <i class="fas fa-graduation-cap text-primary"></i> Sign In
                    </h3>

                    <c:if test="${param.error != null}">
                        <div class="alert alert-danger">
                            <i class="fas fa-exclamation-circle"></i> Invalid username or password.
                        </div>
                    </c:if>
                    <c:if test="${param.logout != null}">
                        <div class="alert alert-success">
                            <i class="fas fa-check-circle"></i> You have been signed out successfully.
                        </div>
                    </c:if>

                    <form action="<c:url value='/login' />" method="post">
                        <div class="form-group">
                            <label for="username">Username</label>
                            <input type="text" id="username" name="username"
                                   class="form-control" placeholder="Enter your username" required autofocus>
                        </div>
                        <div class="form-group">
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password"
                                   class="form-control" placeholder="Enter your password" required>
                        </div>
                        <div class="form-group form-check">
                            <input type="checkbox" class="form-check-input" id="rememberMe" name="remember-me">
                            <label class="form-check-label" for="rememberMe">Remember me</label>
                        </div>
                        <button type="submit" class="btn btn-primary btn-block">
                            <i class="fas fa-sign-in-alt"></i> Sign In
                        </button>
                    </form>

                    <hr>
                    <p class="text-center text-muted mb-0">
                        Don't have an account? <a href="<c:url value='/register' />">Register here</a>
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>
