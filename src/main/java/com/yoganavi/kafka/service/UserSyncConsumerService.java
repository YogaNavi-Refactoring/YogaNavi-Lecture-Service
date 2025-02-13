package com.yoganavi.kafka.service;

import com.yoganavi.kafka.entity.UserEventLog;
import com.yoganavi.kafka.event.UserEvent;
import com.yoganavi.kafka.event.UserEvent.EventStatus;
import com.yoganavi.kafka.repository.UserEventLogRepository;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncConsumerService {

    private final UserRepository userRepository;
    private final UserEventLogRepository eventLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DlqManagementService dlqManagementService;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    @Value("${spring.kafka.topics.user-sync-result}")
    private String resultTopic;

    @Value("${test.simulate.failure:true}")
    private boolean simulateFailure;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topics.user-created}")
    public void handleUserCreatedEvent(UserEvent event) {
        log.info("회원 가입 이벤트 수신: {}", event);
        UserEventLog eventLog = null;

        try {
            eventLog = UserEventLog.start(event.getUserId(), "USER_CREATED");
            eventLogRepository.save(eventLog);

            if (simulateFailure && retryCount.get() < 3) { // 3번의 재시도 후 실패하면 DLQ로 이동
                String errorMessage = "일시적인 서비스 장애 발생";
                log.error("일시적인 서비스 장애 시뮬레이션. 현재 재시도 횟수: {}", retryCount.incrementAndGet());
                handleFailure(event, eventLog, new RuntimeException(errorMessage));
                throw new RuntimeException(errorMessage);
            }

            if (userRepository.existsById(event.getUserId())) {
                handleDuplicateUser(event, eventLog);
                return;
            }

            Users user = createAndSaveUser(event);
            handleSuccess(event, eventLog);

            retryCount.set(0);

        } catch (Exception e) {
            handleFailure(event, eventLog, e);
            throw e;
        }
    }

    private Users createAndSaveUser(UserEvent event) {
        Users user = new Users();
        user.setUserId(event.getUserId());
        user.setEmail(event.getEmail());
        user.setNickname(event.getNickname());
        user.setProfileImageUrl(event.getProfileImageUrl());
        user.setProfileImageUrlSmall(event.getProfileImageUrlSmall());
        user.setRole(event.getRole());
        user.setContent(event.getContent());
        user.setIsDeleted(false);

        return userRepository.save(user);
    }

    private void handleSuccess(UserEvent event, UserEventLog eventLog) {
        sendSyncResult(event, EventStatus.COMPLETED, null);
        eventLog.complete();
        eventLogRepository.save(eventLog);
        log.info("회원 가입 사용자 정보 동기화 성공: userId={}, transactionId={}",
            event.getUserId(), event.getTransactionId());
    }

    private void handleDuplicateUser(UserEvent event, UserEventLog eventLog) {
        sendSyncResult(event, EventStatus.COMPLETED, "이미 동기화된 사용자");
        eventLog.complete();
        eventLogRepository.save(eventLog);
        log.info("이미 존재하는 사용자: userId={}, transactionId={}",
            event.getUserId(), event.getTransactionId());
    }

    private void handleFailure(UserEvent event, UserEventLog eventLog, Exception e) {
        log.error("회원 가입 사용자 정보 동기화 실패: {}", e.getMessage());

        if (eventLog != null) {
            eventLog.fail(e.getMessage());
            eventLogRepository.save(eventLog);
        }

        sendSyncResult(event, EventStatus.FAILED, e.getMessage());

        if (retryCount.get() >= 3) {
            log.error("최대 재시도 횟수 초과. DLQ로 이동: userId={}, transactionId={}",
                event.getUserId(), event.getTransactionId());
            dlqManagementService.addDlqMessage(event, e);
        }
    }

    @KafkaListener(topics = "${spring.kafka.topics.user-created-dlq}")
    public void handleDlqMessage(UserEvent event) { //DLQ 토픽으로 이동된 메시지 추적, 관리
        log.error("DLQ 메시지 상세 정보: ");
        log.error("Transaction ID: {}", event.getTransactionId());
        log.error("User ID: {}", event.getUserId());
        log.error("Email: {}", event.getEmail());
        log.error("Nickname: {}", event.getNickname());
        log.error("Event Type: {}", event.getEventType());
        log.error("Event Status: {}", event.getStatus());
        log.error("Error Message: {}", event.getErrorMessage());
        log.error("Timestamp: {}", event.getTimestamp());

        dlqManagementService.addDlqMessage(event,
            new RuntimeException("메시지가 DLQ로 이동됨"));
    }

    private void sendSyncResult(UserEvent originalEvent, EventStatus status, String errorMessage) {
        UserEvent resultEvent = UserEvent.builder()
            .transactionId(originalEvent.getTransactionId())
            .userId(originalEvent.getUserId())
            .eventType(originalEvent.getEventType())
            .status(status)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send(resultTopic, String.valueOf(resultEvent.getUserId()), resultEvent);
        log.info("동기화 결과 이벤트 발행. 상태: {}, 사용자: {}, transactionId: {}",
            status, resultEvent.getUserId(), resultEvent.getTransactionId());
    }

    public void resetSimulation() {
        retryCount.set(0);
        simulateFailure = false;
        dlqManagementService.clearDlqMessages();
    }

    @Transactional
    public void retryDlqMessages() {
        List<DlqManagementService.DlqMessageEntry> messagesToRetry = dlqManagementService.getDlqMessages();

        for (DlqManagementService.DlqMessageEntry entry : messagesToRetry) {
            try {
                dlqManagementService.updateDlqMessageStatus(
                    entry.getTransactionId(),
                    DlqManagementService.DlqStatus.PROCESSING,
                    null
                );

                handleUserCreatedEvent(entry.getEvent());

                dlqManagementService.removeDlqMessage(entry.getTransactionId());

            } catch (Exception e) {
                log.error("DLQ 메시지 재처리 실패: transactionId={}, error={}",
                    entry.getTransactionId(), e.getMessage());

                entry.incrementRetryCount();
                if (entry.getRetryCount() >= 3) {
                    dlqManagementService.updateDlqMessageStatus(
                        entry.getTransactionId(),
                        DlqManagementService.DlqStatus.DEAD,
                        e.getMessage()
                    );
                } else {
                    dlqManagementService.updateDlqMessageStatus(
                        entry.getTransactionId(),
                        DlqManagementService.DlqStatus.RETRY_FAILED,
                        e.getMessage()
                    );
                }
            }
        }
    }
}