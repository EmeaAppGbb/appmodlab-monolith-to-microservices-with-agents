package com.eduverse.config;

import com.eduverse.scheduler.EnrollmentReminderJob;
import com.eduverse.scheduler.ReportGenerationJob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class SchedulerConfig {

    // EnrollmentReminderJob - daily at 9 AM
    @Bean
    public JobDetailFactoryBean enrollmentReminderJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(EnrollmentReminderJob.class);
        factory.setName("enrollmentReminderJob");
        factory.setGroup("enrollment");
        factory.setDescription("Sends enrollment reminder emails daily");
        factory.setDurability(true);
        return factory;
    }

    @Bean
    public CronTriggerFactoryBean enrollmentReminderTrigger() {
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(enrollmentReminderJobDetail().getObject());
        factory.setName("enrollmentReminderTrigger");
        factory.setGroup("enrollment");
        factory.setCronExpression("0 0 9 * * ?"); // daily at 9 AM
        return factory;
    }

    // ReportGenerationJob - weekly on Monday at midnight
    @Bean
    public JobDetailFactoryBean reportGenerationJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(ReportGenerationJob.class);
        factory.setName("reportGenerationJob");
        factory.setGroup("reporting");
        factory.setDescription("Generates weekly reports every Monday at midnight");
        factory.setDurability(true);
        return factory;
    }

    @Bean
    public CronTriggerFactoryBean reportGenerationTrigger() {
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(reportGenerationJobDetail().getObject());
        factory.setName("reportGenerationTrigger");
        factory.setGroup("reporting");
        factory.setCronExpression("0 0 0 ? * MON"); // every Monday at midnight
        return factory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobDetails(
                enrollmentReminderJobDetail().getObject(),
                reportGenerationJobDetail().getObject()
        );
        factory.setTriggers(
                enrollmentReminderTrigger().getObject(),
                reportGenerationTrigger().getObject()
        );
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);
        return factory;
    }
}
