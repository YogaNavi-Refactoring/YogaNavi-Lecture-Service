package com.yoganavi.lecture.testUser;

import com.yoganavi.kafka.entity.UserEventLog;
import com.yoganavi.kafka.repository.UserEventLogRepository;
import com.yoganavi.kafka.service.DlqManagementService;
import com.yoganavi.kafka.service.UserSyncConsumerService;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestUserController {

    private final UserRepository userRepository;
    private final UserEventLogRepository eventLogRepository;
    private final UserSyncConsumerService userSyncConsumerService;
    private final DlqManagementService dlqManagementService;

    @GetMapping("/test/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/test/email/{email}")
    public ResponseEntity<Users> getUserByEmail(@PathVariable String email) {
        log.info("getUserByEmail: {}", email);
        Optional<Users> user = userRepository.findByEmail(email);
        log.info("Found user: {}", user.isPresent());
        return user.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/dlq-count")
    public ResponseEntity<Map<String, Object>> getDlqCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", dlqManagementService.getDlqCount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/reset-simulation")
    public ResponseEntity<Void> resetSimulation() {
        userSyncConsumerService.resetSimulation();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/dlq-messages")
    public ResponseEntity<Map<String, Object>> getDlqMessages() {
        Map<String, Object> response = new HashMap<>();
        response.put("messages", dlqManagementService.getDlqMessages());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/dlq-message/{transactionId}")
    public ResponseEntity<Map<String, Object>> getDlqMessage(@PathVariable String transactionId) {
        DlqManagementService.DlqMessageEntry entry = dlqManagementService.getDlqMessage(
            transactionId);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", entry);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/users/{userId}")
    public ResponseEntity<Users> getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/dlq/retry")
    public ResponseEntity<Map<String, Object>> retryDlqMessages() {
        int beforeCount = dlqManagementService.getDlqCount();
        userSyncConsumerService.retryDlqMessages();
        int afterCount = dlqManagementService.getDlqCount();

        Map<String, Object> response = new HashMap<>();
        response.put("processedCount", beforeCount - afterCount);
        response.put("remainingCount", afterCount);

        log.info("DLQ 재처리 결과 - 처리된 메시지: {}, 남은 메시지: {}",
            beforeCount - afterCount, afterCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/event-logs/{userId}")
    public ResponseEntity<Map<String, Object>> getEventLogs(@PathVariable Long userId) {
        List<UserEventLog> eventLogs = eventLogRepository.findByUserId(userId);
        if (eventLogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 가장 최근 로그 반환
        UserEventLog latestLog = eventLogs.get(eventLogs.size() - 1);
        Map<String, Object> response = new HashMap<>();
        response.put("status", latestLog.getStatus());
        response.put("errorMessage", latestLog.getErrorMessage());
        response.put("createdAt", latestLog.getCreatedAt());
        response.put("completedAt", latestLog.getCompletedAt());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/dlq-message/{transactionId}")
    public ResponseEntity<Void> removeDlqMessage(@PathVariable String transactionId) {
        dlqManagementService.removeDlqMessage(transactionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/dlq-stats")
    public ResponseEntity<Map<String, Object>> getDlqStats() {
        List<DlqManagementService.DlqMessageEntry> messages = dlqManagementService.getDlqMessages();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", messages.size());
        stats.put("pendingCount", messages.stream()
            .filter(m -> m.getStatus() == DlqManagementService.DlqStatus.PENDING)
            .count());
        stats.put("processingCount", messages.stream()
            .filter(m -> m.getStatus() == DlqManagementService.DlqStatus.PROCESSING)
            .count());
        stats.put("failedCount", messages.stream()
            .filter(m -> m.getStatus() == DlqManagementService.DlqStatus.RETRY_FAILED)
            .count());
        stats.put("deadCount", messages.stream()
            .filter(m -> m.getStatus() == DlqManagementService.DlqStatus.DEAD)
            .count());

        return ResponseEntity.ok(stats);
    }
}