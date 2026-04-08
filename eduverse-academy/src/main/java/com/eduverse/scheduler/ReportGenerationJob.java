package com.eduverse.scheduler;

import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.model.Payment;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.PaymentRepository;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Quartz job that generates weekly platform reports by aggregating data
 * from enrollments, courses, and payments.
 *
 * MONOLITH ANTI-PATTERN: This single batch job reaches across three bounded
 * contexts (enrollment, course catalog, billing) to produce a consolidated
 * report. It directly queries every repository, creating a massive fan-out
 * of cross-domain data access that would be impossible in a microservices
 * architecture without an event-driven or CQRS approach.
 */
public class ReportGenerationJob extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationJob.class);

    private static final DateTimeFormatter REPORT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Cross-domain repository access — each belongs to a different bounded context
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        LocalDateTime reportTime = LocalDateTime.now();
        logger.info("Starting ReportGenerationJob — weekly report for {}", reportTime.format(REPORT_DATE_FMT));

        try {
            // ---- Enrollment statistics (enrollment domain) ----
            List<Enrollment> allEnrollments = enrollmentRepository.findAll();
            long totalEnrollments = allEnrollments.size();
            long activeEnrollments = allEnrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE).count();
            long completedEnrollments = allEnrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.COMPLETED).count();
            long droppedEnrollments = allEnrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.DROPPED).count();

            BigDecimal completionRate = totalEnrollments > 0
                    ? BigDecimal.valueOf(completedEnrollments)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalEnrollments), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // ---- Course statistics (course catalog domain) ----
            List<Course> allCourses = courseRepository.findAll();
            long totalCourses = allCourses.size();
            long publishedCourses = allCourses.stream()
                    .filter(c -> c.getStatus() == Course.Status.PUBLISHED).count();
            long draftCourses = allCourses.stream()
                    .filter(c -> c.getStatus() == Course.Status.DRAFT).count();
            long archivedCourses = allCourses.stream()
                    .filter(c -> c.getStatus() == Course.Status.ARCHIVED).count();

            // ---- Payment / revenue statistics (billing domain) ----
            List<Payment> allPayments = paymentRepository.findAll();
            long totalPayments = allPayments.size();

            BigDecimal totalRevenue = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.Status.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRefunded = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.Status.REFUNDED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long failedPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.Status.FAILED).count();

            // ---- Log consolidated report ----
            logger.info("============================================================");
            logger.info("        EDUVERSE ACADEMY — WEEKLY PLATFORM REPORT");
            logger.info("        Generated: {}", reportTime.format(REPORT_DATE_FMT));
            logger.info("============================================================");
            logger.info("ENROLLMENT STATISTICS:");
            logger.info("  Total Enrollments:     {}", totalEnrollments);
            logger.info("  Active:                {}", activeEnrollments);
            logger.info("  Completed:             {}", completedEnrollments);
            logger.info("  Dropped:               {}", droppedEnrollments);
            logger.info("  Completion Rate:       {}%", completionRate);
            logger.info("------------------------------------------------------------");
            logger.info("COURSE CATALOG:");
            logger.info("  Total Courses:         {}", totalCourses);
            logger.info("  Published:             {}", publishedCourses);
            logger.info("  Draft:                 {}", draftCourses);
            logger.info("  Archived:              {}", archivedCourses);
            logger.info("------------------------------------------------------------");
            logger.info("REVENUE & PAYMENTS:");
            logger.info("  Total Payments:        {}", totalPayments);
            logger.info("  Total Revenue:         ${}", totalRevenue);
            logger.info("  Total Refunded:        ${}", totalRefunded);
            logger.info("  Failed Payments:       {}", failedPayments);
            logger.info("============================================================");

            logger.info("ReportGenerationJob completed successfully");

        } catch (Exception e) {
            logger.error("ReportGenerationJob failed with unexpected error", e);
            throw new JobExecutionException("ReportGenerationJob failed", e);
        }
    }
}
