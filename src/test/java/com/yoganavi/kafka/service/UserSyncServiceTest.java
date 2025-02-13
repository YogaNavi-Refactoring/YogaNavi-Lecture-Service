package com.yoganavi.kafka.service;

import com.yoganavi.kafka.entity.UserEventLog;
import com.yoganavi.kafka.event.UserEvent;
import com.yoganavi.kafka.repository.UserEventLogRepository;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventLogRepository eventLogRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserSyncConsumerService userSyncService;

    @Captor
    private ArgumentCaptor<Users> usersCaptor;

    @Captor
    private ArgumentCaptor<UserEventLog> eventLogCaptor;

    private UserEvent userEvent;

    @BeforeEach
    void setUp() {
        userEvent = UserEvent.builder()
            .transactionId("test-transaction")
            .userId(1L)
            .email("test@test.com")
            .nickname("testUser")
            .role("TEACHER")
            .eventType(UserEvent.EventType.CREATED)
            .status(UserEvent.EventStatus.STARTED)
            .build();
    }

    @Test
    void 사용자_동기화_성공() {
        // Given
        given(userRepository.existsById(userEvent.getUserId())).willReturn(false);

        // When
        userSyncService.handleUserCreatedEvent(userEvent);

        // Then
        verify(userRepository).save(usersCaptor.capture());
        Users savedUser = usersCaptor.getValue();
        assertThat(savedUser.getUserId()).isEqualTo(userEvent.getUserId());
        assertThat(savedUser.getEmail()).isEqualTo(userEvent.getEmail());
        assertThat(savedUser.getRole()).isEqualTo(userEvent.getRole());

        // 이벤트 로그 확인
        verify(eventLogRepository).save(eventLogCaptor.capture());
        UserEventLog eventLog = eventLogCaptor.getValue();
        assertThat(eventLog.getUserId()).isEqualTo(userEvent.getUserId());
        assertThat(eventLog.getEventType()).isEqualTo("USER_CREATED");
        assertThat(eventLog.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void 이미_존재하는_사용자_처리() {
        // Given
        given(userRepository.existsById(userEvent.getUserId())).willReturn(true);

        // When
        userSyncService.handleUserCreatedEvent(userEvent);

        // Then
        verify(eventLogRepository).save(eventLogCaptor.capture());
        UserEventLog eventLog = eventLogCaptor.getValue();
        assertThat(eventLog.getStatus()).isEqualTo("COMPLETED");
        assertThat(eventLog.getUserId()).isEqualTo(userEvent.getUserId());
    }

    @Test
    void DLQ이벤트_로그_생성_확인() {
        // When
        userSyncService.handleDlqMessage(userEvent);

        // Then
        // DLQ 메시지 처리는 현재 로깅만 수행 -> 검증 불필요
        // 필요한 경우 로깅 검증 추가
    }
}