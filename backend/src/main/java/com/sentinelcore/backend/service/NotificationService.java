package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.NotificationDTO;
import com.sentinelcore.backend.entity.Notification;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationDTO> getUnreadNotifications() {
        log.info("Fetching unread notifications");
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<NotificationDTO> getAllNotifications() {
        log.info("Fetching all notifications");
        return notificationRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public void createNotification(String message, String severity) {
        log.info("Creating notification: {}", message);
        Notification notification = Notification.builder()
                .message(message)
                .severity(severity)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    public void markAsRead(Long id) {
        log.info("Marking notification {} as read", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        log.info("Marking all notifications as read");
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationDTO convertToDTO(Notification entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .severity(entity.getSeverity())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
