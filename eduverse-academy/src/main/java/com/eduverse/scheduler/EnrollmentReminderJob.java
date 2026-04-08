package com.eduverse.scheduler;

import com.eduverse.model.Enrollment;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.service.NotificationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quartz job that sends reminder notifications to students who haven't
 * accessed their active enrollments in over 7 days.
 *
 * MONOLITH ANTI-PATTERN: This batch job directly autowires repositories and
 * services from multiple domains (enrollment, notification). It reaches across
 * bounded contexts to query enrollment data and trigger notifications in a
 * single tightly-coupled unit — making it impossible to scale, deploy, or
 * maintain these concerns independently.
 */
public class EnrollmentReminderJob extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentReminderJob.class);

    private static final int INACTIVE_DAYS_THRESHOLD = 7;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // Tight coupling: batch job directly depends on NotificationService
    @Autowired
    private NotificationService notificationService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("Starting EnrollmentReminderJob — scanning for inactive enrollments");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);

            // Query all active enrollments and filter in-memory — inefficient but
            // typical of monolith batch jobs that bypass proper query optimization
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStatus(Enrollment.Status.ACTIVE);

            List<Enrollment> inactiveEnrollments = activeEnrollments.stream()
                    .filter(e -> e.getLastAccessed() != null && e.getLastAccessed().isBefore(cutoffDate))
                    .collect(Collectors.toList());

            logger.info("Found {} active enrollments, {} inactive for >{}  days",
                    activeEnrollments.size(), inactiveEnrollments.size(), INACTIVE_DAYS_THRESHOLD);

            int remindersSent = 0;
            int failureCount = 0;

            for (Enrollment enrollment : inactiveEnrollments) {
                try {
                    // Tight coupling: directly calling NotificationService with
                    // enrollment domain knowledge baked into the message
                    String subject = "We miss you! Continue your learning journey";
                    String message = "Hi there,\n\n"
                            + "We noticed you haven't accessed your course (enrollment #"
                            + enrollment.getId() + ") in over " + INACTIVE_DAYS_THRESHOLD + " days.\n\n"
                            + "Your current progress is " + enrollment.getProgressPercent() + "%. "
                            + "Don't lose momentum — log in and pick up where you left off!\n\n"
                            + "Happy Learning,\n"
                            + "The EduVerse Academy Team";

                    notificationService.sendEmail(enrollment.getStudentId(), subject, message);
                    remindersSent++;

                    logger.debug("Sent reminder to studentId={} for enrollmentId={}",
                            enrollment.getStudentId(), enrollment.getId());
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Failed to send reminder for enrollmentId={}: {}",
                            enrollment.getId(), e.getMessage(), e);
                }
            }

            logger.info("EnrollmentReminderJob completed — sent {} reminders, {} failures",
                    remindersSent, failureCount);

        } catch (Exception e) {
            logger.error("EnrollmentReminderJob failed with unexpected error", e);
            throw new JobExecutionException("EnrollmentReminderJob failed", e);
        }
    }
}
