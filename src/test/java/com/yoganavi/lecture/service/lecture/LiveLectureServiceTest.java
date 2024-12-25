package com.yoganavi.lecture.service.lecture;

import com.yoganavi.lecture.live_lecture.service.lecture.LiveLectureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.springframework.boot.test.context.SpringBootTest;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.LiveLectures;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import com.yoganavi.lecture.common.entity.Users;

import com.yoganavi.lecture.live_lecture.repository.LiveLecturesRepository;
import com.yoganavi.lecture.live_lecture.repository.MyLiveLectureRepository;
import com.yoganavi.lecture.live_lecture.repository.UserRepository;

import com.yoganavi.lecture.live_lecture.dto.LiveLectureInfoDto;
import com.yoganavi.lecture.live_lecture.dto.LiveLectureResponseDto;
import com.yoganavi.lecture.common.util.TimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SpringBootTest
class LiveLectureServiceTest {

    @Mock
    private LiveLecturesRepository liveLecturesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MyLiveLectureRepository myLiveLectureRepository;
    @Mock
    private TimeUtil timeUtil;
    @InjectMocks
    private LiveLectureServiceImpl liveLectureService;

    private Users testUser;
    private LiveLectures testLecture;
    private LectureSchedule testSchedule;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // 테스트 유저 생성
        testUser = new Users();
        testUser.setUserId(1L);
        testUser.setNickname("테스트강사");

        // 테스트 강의 생성
        testLecture = new LiveLectures();
        testLecture.setLiveId(1L);
        testLecture.setLiveTitle("테스트강의");
        testLecture.setLiveContent("테스트내용");
        testLecture.setMaxLiveNum(10);
        testLecture.setRegDate(now);
        testLecture.setUser(testUser);
        testLecture.setIsOnAir(false);
        testLecture.setIsDeleted(false);

        // 테스트 스케줄 생성
        testSchedule = new LectureSchedule();
        testSchedule.setScheduleId(1L);
        testSchedule.setLecture(testLecture);
        testSchedule.setStartTime(now.plusDays(1));
        testSchedule.setEndTime(now.plusDays(1).plusHours(1));

        List<LectureSchedule> schedules = new ArrayList<>();
        schedules.add(testSchedule);
        testLecture.setSchedules(schedules);

        // TimeUtils Mocking
        when(timeUtil.toLocalDate(anyLong())).thenReturn(now.toLocalDate());
        when(timeUtil.toLocalTime(anyLong())).thenReturn(now.toLocalTime());
        when(timeUtil.toEpochMilli(any())).thenReturn(
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * 강의 생성 관련 테스트
     */
    @Nested
    @DisplayName("강의 생성 테스트")
    class CreateLectureTest {

        @Test
        void 정상_강의_생성() {
            // given
            LiveLectureInfoDto dto = new LiveLectureInfoDto();
            dto.setUserId(1L);
            dto.setLiveTitle("새 강의");
            dto.setLiveContent("새 내용");
            dto.setMaxLiveNum(10);
            dto.setStartDate(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setEndDate(
                now.plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setStartTime(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setEndTime(
                now.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setAvailableDay("MON,WED,FRI");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(liveLecturesRepository.save(any(LiveLectures.class))).thenReturn(testLecture);

            // when
            liveLectureService.createLiveLecture(dto);

            // then
            verify(liveLecturesRepository, times(1)).save(any(LiveLectures.class));
            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        void 유효하지_않은_사용자_강의_생성() {
            // given
            LiveLectureInfoDto dto = new LiveLectureInfoDto();
            dto.setUserId(999L);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when, then
            assertThrows(IllegalArgumentException.class,
                () -> liveLectureService.createLiveLecture(dto));
        }
    }

    /**
     * 강의 수정 관련 테스트
     */
    @Nested
    class UpdateLectureTest {

        @Test
        void 기본정보_수정() {
            // given
            LiveLectureInfoDto dto = new LiveLectureInfoDto();
            dto.setLiveId(1L);
            dto.setLiveTitle("수정된 제목");
            dto.setLiveContent("수정된 내용");
            dto.setMaxLiveNum(20);

            when(liveLecturesRepository.findById(1L)).thenReturn(Optional.of(testLecture));
            when(liveLecturesRepository.save(any(LiveLectures.class))).thenReturn(
                testLecture); // 이 부분 추가

            // when
            liveLectureService.updateLiveLecture(dto);

            // then
            assertEquals("수정된 제목", testLecture.getLiveTitle());
            assertEquals("수정된 내용", testLecture.getLiveContent());
            assertEquals(20, testLecture.getMaxLiveNum());
        }

        @Test
        void 과거_스케줄_유지() {
            // given
            LectureSchedule pastSchedule = new LectureSchedule();
            pastSchedule.setStartTime(now.minusDays(1));
            pastSchedule.setEndTime(now.minusDays(1).plusHours(1));

            testLecture.getSchedules().add(pastSchedule);

            LiveLectureInfoDto dto = new LiveLectureInfoDto();
            dto.setLiveId(1L);
            dto.setStartDate(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setEndDate(
                now.plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            when(liveLecturesRepository.findById(1L)).thenReturn(Optional.of(testLecture));
            when(liveLecturesRepository.save(any(LiveLectures.class))).thenReturn(
                testLecture); // 이 부분 추가

            // when
            liveLectureService.updateLiveLecture(dto);

            // then
            assertTrue(testLecture.getSchedules().contains(pastSchedule));
        }

        /**
         * 강의 삭제 관련 테스트
         */
        @Nested
        class DeleteLectureTest {

            @Test
            void 수강생없는_강의_삭제() {
                // given
                when(liveLecturesRepository.findById(1L)).thenReturn(Optional.of(testLecture));
                when(myLiveLectureRepository.findByLiveLectureLiveId(1L)).thenReturn(
                    new ArrayList<>());

                // when
                liveLectureService.deleteLiveLectureById(1L);

                // then
                assertTrue(testLecture.getIsDeleted());
                assertTrue(testLecture.getSchedules().isEmpty());
            }

            @Test
            void 진행중_강의_삭제_불가() {
                // given
                testLecture.setIsOnAir(true);
                when(liveLecturesRepository.findById(1L)).thenReturn(Optional.of(testLecture));

                // when, then
                assertThrows(IllegalStateException.class,
                    () -> liveLectureService.deleteLiveLectureById(1L));
            }
        }

        /**
         * 강의 조회 관련 테스트
         */
        @Nested
        class GetLectureTest {

            @Test
            void 강의_상세_조회() {
                // given
                when(liveLecturesRepository.findByIdWithUserAndSchedules(1L))
                    .thenReturn(Optional.of(testLecture));

                // when
                LiveLectureResponseDto result = liveLectureService.getLiveLectureById(1L);

                // then
                assertEquals(testLecture.getLiveId(), result.getLiveId());
                assertEquals(testLecture.getLiveTitle(), result.getLiveTitle());
                assertEquals(testLecture.getUser().getNickname(), result.getNickname());
            }

            @Test
            void 강사_강의목록_조회() {
                // given
                List<LiveLectures> lectures = Arrays.asList(testLecture);
                when(liveLecturesRepository.findByUserId(1L)).thenReturn(lectures);

                // when
                List<LiveLectureResponseDto> results = liveLectureService.getLiveLecturesByUserId(
                    1L);

                // then
                assertEquals(1, results.size());
                assertEquals(testLecture.getLiveId(), results.get(0).getLiveId());
            }
        }

        /**
         * 스케줄 관리 관련 테스트
         */
        @Nested
        class ScheduleManagementTest {

            @Test
            void 수강생_근접시간_재배치() {
                // given
                MyLiveLecture enrollment = new MyLiveLecture();
                enrollment.setUserId(2L);
                enrollment.setLectureSchedule(testSchedule);

                LectureSchedule newSchedule = new LectureSchedule();
                newSchedule.setStartTime(testSchedule.getStartTime().plusHours(1));
                newSchedule.setEndTime(testSchedule.getEndTime().plusHours(1));

                List<MyLiveLecture> enrollments = Collections.singletonList(enrollment);
                Map<LectureSchedule, List<MyLiveLecture>> enrollmentMap = new HashMap<>();
                enrollmentMap.put(testSchedule, enrollments);

                // when
                liveLectureService.redistributeStudents(enrollmentMap,
                    Collections.singletonList(newSchedule));

                // then
                verify(myLiveLectureRepository, times(1)).save(any(MyLiveLecture.class));
            }
        }
    }
}