package com.eduverse.repository;

import com.eduverse.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndSent(Long userId, Boolean sent);

    List<Notification> findBySent(Boolean sent);
}
