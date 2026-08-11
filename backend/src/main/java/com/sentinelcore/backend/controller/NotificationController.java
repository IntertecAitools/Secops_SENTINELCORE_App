package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.NotificationDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications(
            @RequestParam(defaultValue = "false") boolean all) {
        List<NotificationDTO> list = all ? notificationService.getAllNotifications() : notificationService.getUnreadNotifications();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Object>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
