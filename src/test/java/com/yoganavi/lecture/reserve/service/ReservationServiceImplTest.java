package com.yoganavi.lecture.reserve.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.LiveLectures;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.LiveLecturesRepository;
import com.yoganavi.lecture.common.repository.MyLiveLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.reserve.dto.LiveLectureDto;
import com.yoganavi.lecture.reserve.dto.ReservationRequestDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Mock
    private LiveLecturesRepository liveLecturesRepository;

    @Mock
    private MyLiveLectureRepository myLiveLectureRepository;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<MyLiveLecture> reservationCaptor;

    private Users 학생;
    private Users 강사;
    private LiveLectures 강의;
    private LectureSchedule 스케줄;
    private ReservationRequestDto 수강신청요청;
    private LocalDateTime 현재시간;

    @BeforeEach
    void setUp() {
        현재시간 = LocalDateTime.now();

        학생 = new Users();
        학생.setUserId(1L);
        학생.setEmail("student@test.com");

        강사 = new Users();
        강사.setUserId(2L);
        강사.setEmail("teacher@test.com");

        강의 = new LiveLectures();
        강의.setLiveId(1L);
        강의.setLiveTitle("테스트 강의");
        강의.setLiveContent("테스트 내용");
        강의.setMaxLiveNum(5);
        강의.setUser(강사);
        강의.setIsDeleted(false);
        강의.setRegDate(현재시간);  // 등록일자 설정

        스케줄 = LectureSchedule.createSchedule(
            현재시간.plusDays(1).toLocalDate(),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0)
        );
        스케줄.setLecture(강의);

        List<LectureSchedule> schedules = new ArrayList<>();
        schedules.add(스케줄);
        강의.setSchedules(schedules);

        수강신청요청 = new ReservationRequestDto();
        수강신청요청.setLiveId(1L);
        수강신청요청.setStartDate(현재시간.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        수강신청요청.setEndDate(현재시간.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Nested
    @DisplayName("수강신청 생성")
    class 수강신청생성 {


        @Test
        @DisplayName("존재하지 않는 사용자로 수강신청 실패")
        void 존재하지않는사용자_수강신청_실패() {
            // given
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                reservationService.createReservation(999L, 수강신청요청))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("삭제된 강의로 수강신청 실패")
        void 삭제된강의_수강신청_실패() {
            // given
            강의.setIsDeleted(true);
            when(userRepository.findById(학생.getUserId())).thenReturn(Optional.of(학생));
            when(liveLecturesRepository.findByIdWithUserAndSchedules(강의.getLiveId()))
                .thenReturn(Optional.of(강의));

            // when & then
            assertThatThrownBy(() ->
                reservationService.createReservation(학생.getUserId(), 수강신청요청))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제된 강의입니다.");
        }

        @Test
        @DisplayName("수강 정원 초과로 수강신청 실패")
        void 수강정원초과_수강신청_실패() {
            // given
            when(userRepository.findById(학생.getUserId())).thenReturn(Optional.of(학생));
            when(liveLecturesRepository.findByIdWithUserAndSchedules(강의.getLiveId()))
                .thenReturn(Optional.of(강의));
            when(myLiveLectureRepository.countByLiveLectureId(eq(강의.getLiveId())))
                .thenReturn((long) 강의.getMaxLiveNum());

            // when & then
            assertThatThrownBy(() ->
                reservationService.createReservation(학생.getUserId(), 수강신청요청))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수강 인원이 초과되었습니다.");
        }

        @Test
        @DisplayName("자신의 강의 수강신청 실패")
        void 자신의강의_수강신청_실패() {
            // given
            when(userRepository.findById(강사.getUserId())).thenReturn(Optional.of(강사));
            when(liveLecturesRepository.findByIdWithUserAndSchedules(강의.getLiveId()))
                .thenReturn(Optional.of(강의));

            // when & then
            assertThatThrownBy(() ->
                reservationService.createReservation(강사.getUserId(), 수강신청요청))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 강의는 수강신청할 수 없습니다.");
        }

        @Test
        @DisplayName("시간 중복되는 강의 수강신청 실패")
        void 시간중복_수강신청_실패() {
            // given
            LocalDateTime 시작시간 = 스케줄.getStartTime();

            when(userRepository.findById(eq(학생.getUserId()))).thenReturn(Optional.of(학생));
            when(liveLecturesRepository.findByIdWithUserAndSchedules(eq(강의.getLiveId())))
                .thenReturn(Optional.of(강의));
            when(myLiveLectureRepository.findOverlappingLectures(
                eq(학생.getUserId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )).thenReturn(List.of(new MyLiveLecture()));  // 중복된 강의가 있다고 가정

            // when & then
            assertThatThrownBy(() ->
                reservationService.createReservation(학생.getUserId(), 수강신청요청))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 시간에 이미 수강중인 강의가 있습니다.");
        }
    }

    @Nested
    @DisplayName("강의 조회")
    class 강의조회 {

        @Test
        @DisplayName("전체 라이브 강의 조회 성공")
        void 전체_라이브강의_조회성공() {
            // given
            when(liveLecturesRepository.findAvailableLectures(eq(0), any(LocalDateTime.class)))
                .thenReturn(List.of(강의));
            when(myLiveLectureRepository.countByLiveLectureId(eq(강의.getLiveId()))).thenReturn(0L);

            // when
            List<LiveLectureDto> 결과 = reservationService.getAllLiveLectures(0);

            // then
            assertThat(결과).hasSize(1);
            assertThat(결과.get(0).getLiveId()).isEqualTo(강의.getLiveId());
            assertThat(결과.get(0).getLiveTitle()).isEqualTo(강의.getLiveTitle());
        }

        @Test
        @DisplayName("특정 강사의 라이브 강의 조회 성공")
        void 강사별_라이브강의_조회성공() {
            // given
            강의.setMaxLiveNum(1);  // method=0일 때는 1:1 강의
            스케줄.setStartTime(현재시간.plusDays(1));  // 미래 시간으로
            스케줄.setEndTime(현재시간.plusDays(1).plusHours(1));

            when(liveLecturesRepository.findByUserId(eq(강사.getUserId())))
                .thenReturn(List.of(강의));
            when(myLiveLectureRepository.countByLiveLectureId(eq(강의.getLiveId()))).thenReturn(0L);

            // when
            var 결과 = reservationService.getLiveLecturesByTeacherAndMethod(강사.getUserId(), 0);

            // then
            assertThat(결과).hasSize(1);
            assertThat(결과.get(0).getLiveId()).isEqualTo(강의.getLiveId());
            assertThat(결과.get(0).getLiveTitle()).isEqualTo(강의.getLiveTitle());
        }

        @Test
        @DisplayName("잘못된 조회 방법으로 강의 조회 실패")
        void 잘못된방법_강의조회_실패() {
            // when & then
            assertThatThrownBy(() ->
                reservationService.getLiveLecturesByTeacherAndMethod(강사.getUserId(), 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바르지 않은 조회 방법입니다.");
        }
    }
}