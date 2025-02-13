package com.yoganavi.kafka.service;

import com.yoganavi.kafka.event.UserEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DlqManagementService {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DlqMessageEntry {

        private UserEvent event;
        private LocalDateTime entryTime;
        private int retryCount;
        private String lastErrorMessage;
        private DlqStatus status;
        private String transactionId;

        public void incrementRetryCount() {
            this.retryCount++;
        }
    }

    public enum DlqStatus {
        PENDING,      // DLQ에 처음 들어온 상태
        PROCESSING,   // 재처리 시도 중
        RETRY_FAILED, // 재처리 실패
        DEAD         // 최대 재시도 횟수 초과
    }

    private final Map<String, DlqMessageEntry> dlqMessages = new ConcurrentHashMap<>();
    private final AtomicInteger dlqMessageCount = new AtomicInteger(0);

    public void addDlqMessage(UserEvent event, Exception cause) {
        DlqMessageEntry entry = new DlqMessageEntry(
            event,
            LocalDateTime.now(),
            0,
            cause.getMessage(),
            DlqStatus.PENDING,
            event.getTransactionId()
        );
        dlqMessages.put(event.getTransactionId(), entry);
        dlqMessageCount.incrementAndGet();
    }

    public List<DlqMessageEntry> getDlqMessages() {
        return new ArrayList<>(dlqMessages.values());
    }

    public DlqMessageEntry getDlqMessage(String transactionId) {
        return dlqMessages.get(transactionId);
    }

    public void removeDlqMessage(String transactionId) {
        if (dlqMessages.remove(transactionId) != null) {
            dlqMessageCount.decrementAndGet();
        }
    }

    public int getDlqCount() {
        return dlqMessageCount.get();
    }

    public void clearDlqMessages() {
        dlqMessages.clear();
        dlqMessageCount.set(0);
    }

    public void updateDlqMessageStatus(String transactionId, DlqStatus status,
        String errorMessage) {
        DlqMessageEntry entry = dlqMessages.get(transactionId);
        if (entry != null) {
            entry.setStatus(status);
            entry.setLastErrorMessage(errorMessage);
        }
    }
}