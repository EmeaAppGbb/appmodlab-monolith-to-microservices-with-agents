<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container mt-4">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/catalog' />">Catalog</a></li>
            <li class="breadcrumb-item active">Checkout</li>
        </ol>
    </nav>

    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-triangle"></i> ${error}
        </div>
    </c:if>

    <h2 class="mb-4"><i class="fas fa-shopping-cart"></i> Checkout</h2>

    <div class="row">
        <!-- Payment Form -->
        <div class="col-md-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-credit-card"></i> Payment Details</h5>
                </div>
                <div class="card-body">
                    <form action="<c:url value='/payment/process' />" method="post" id="paymentForm">
                        <input type="hidden" name="courseId" value="${courseId}" />
                        <input type="hidden" name="stripeToken" id="stripeToken" value="" />
                        <input type="hidden" name="amount" id="amountField" value="${course.price}" />

                        <!-- Cardholder Name -->
                        <div class="form-group">
                            <label for="cardholderName">
                                <i class="fas fa-user"></i> Cardholder Name
                            </label>
                            <input type="text" class="form-control" id="cardholderName"
                                   placeholder="Name on card" required>
                        </div>

                        <!-- Email -->
                        <div class="form-group">
                            <label for="email">
                                <i class="fas fa-envelope"></i> Email Address
                            </label>
                            <input type="email" class="form-control" id="email"
                                   placeholder="your@email.com" required>
                            <small class="form-text text-muted">Receipt will be sent to this email.</small>
                        </div>

                        <!-- Card Number -->
                        <div class="form-group">
                            <label for="cardNumber">
                                <i class="fas fa-credit-card"></i> Card Number
                            </label>
                            <div id="card-element" class="form-control" style="padding-top:10px; height:auto;">
                                <!-- Stripe Elements placeholder -->
                                <input type="text" class="border-0 w-100" id="cardNumber"
                                       placeholder="4242 4242 4242 4242" maxlength="19"
                                       style="outline:none;">
                            </div>
                        </div>

                        <div class="form-row">
                            <div class="form-group col-md-6">
                                <label for="cardExpiry">Expiry Date</label>
                                <input type="text" class="form-control" id="cardExpiry"
                                       placeholder="MM / YY" maxlength="7" required>
                            </div>
                            <div class="form-group col-md-6">
                                <label for="cardCvc">CVC</label>
                                <input type="text" class="form-control" id="cardCvc"
                                       placeholder="123" maxlength="4" required>
                            </div>
                        </div>

                        <div id="card-errors" class="text-danger small mb-3" role="alert"></div>

                        <hr>

                        <!-- Terms -->
                        <div class="custom-control custom-checkbox mb-3">
                            <input type="checkbox" class="custom-control-input" id="agreeTerms" required>
                            <label class="custom-control-label" for="agreeTerms">
                                I agree to the <a href="#" data-toggle="modal" data-target="#termsModal">Terms of Service</a>
                                and <a href="#">Refund Policy</a>.
                            </label>
                        </div>

                        <button type="submit" class="btn btn-success btn-lg btn-block" id="submitPaymentBtn">
                            <i class="fas fa-lock"></i> Pay Now
                            <c:if test="${not empty course.price}">
                                — $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                            </c:if>
                        </button>

                        <p class="text-center text-muted small mt-3">
                            <i class="fas fa-shield-alt"></i> Your payment is secured with 256-bit SSL encryption.
                        </p>
                    </form>
                </div>
            </div>
        </div>

        <!-- Order Summary -->
        <div class="col-md-5">
            <div class="card shadow-sm sticky-top" style="top:80px;">
                <div class="card-header bg-white">
                    <h5 class="mb-0"><i class="fas fa-receipt"></i> Order Summary</h5>
                </div>
                <div class="card-body">
                    <c:if test="${not empty course}">
                        <div class="d-flex align-items-start mb-3">
                            <c:choose>
                                <c:when test="${not empty course.thumbnailUrl}">
                                    <img src="${course.thumbnailUrl}" alt="${course.title}"
                                         class="rounded mr-3" style="width:80px; height:60px; object-fit:cover;">
                                </c:when>
                                <c:otherwise>
                                    <div class="bg-primary text-white rounded mr-3 d-flex align-items-center justify-content-center"
                                         style="width:80px; height:60px;">
                                        <i class="fas fa-book fa-2x"></i>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h6 class="mb-1">${course.title}</h6>
                                <span class="badge badge-secondary">${course.category}</span>
                            </div>
                        </div>
                        <ul class="list-unstyled text-muted small mb-3">
                            <li><i class="fas fa-clock mr-2"></i> ${course.durationHours} hours of content</li>
                            <li><i class="fas fa-layer-group mr-2"></i> ${course.modules.size()} modules</li>
                            <li><i class="fas fa-certificate mr-2"></i> Certificate of completion</li>
                            <li><i class="fas fa-infinity mr-2"></i> Lifetime access</li>
                        </ul>
                    </c:if>

                    <hr>

                    <div class="d-flex justify-content-between mb-2">
                        <span>Subtotal</span>
                        <span>
                            <c:choose>
                                <c:when test="${not empty course.price}">
                                    $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="d-flex justify-content-between mb-2 text-muted small">
                        <span>Tax</span>
                        <span>$0.00</span>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between">
                        <strong class="h5 mb-0">Total</strong>
                        <strong class="h5 mb-0 text-success">
                            <c:choose>
                                <c:when test="${not empty course.price}">
                                    $<fmt:formatNumber value="${course.price}" pattern="#,##0.00" />
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </strong>
                    </div>
                </div>
                <div class="card-footer text-center">
                    <small class="text-muted">
                        <i class="fab fa-cc-visa mr-1"></i>
                        <i class="fab fa-cc-mastercard mr-1"></i>
                        <i class="fab fa-cc-amex mr-1"></i>
                        <i class="fab fa-cc-stripe mr-1"></i>
                        Secure payments by Stripe
                    </small>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Terms Modal -->
<div class="modal fade" id="termsModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Terms of Service</h5>
                <button type="button" class="close" data-dismiss="modal">&times;</button>
            </div>
            <div class="modal-body">
                <p>By purchasing a course on EduVerse Academy, you agree to the following:</p>
                <ul>
                    <li>You will have lifetime access to the purchased course content.</li>
                    <li>Course content is for personal, non-commercial use only.</li>
                    <li>Refunds are available within 30 days of purchase if less than 25% of the course has been completed.</li>
                </ul>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-dismiss="modal">I Understand</button>
            </div>
        </div>
    </div>
</div>

<script>
    // Simulate Stripe token generation for the payment form
    document.getElementById('paymentForm').addEventListener('submit', function(e) {
        var tokenField = document.getElementById('stripeToken');
        if (!tokenField.value) {
            e.preventDefault();
            // In production, Stripe.js would create a real token here.
            // Simulating token creation for this monolith demo.
            tokenField.value = 'tok_demo_' + Date.now();
            this.submit();
        }
    });
</script>
