package com.yoganavi.live_lecture.service.history;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yoganavi.live_lecture.common.entity.LectureSchedule;
import com.yoganavi.live_lecture.common.entity.LiveLectures;
import com.yoganavi.live_lecture.common.entity.MyLiveLecture;
import com.yoganavi.live_lecture.common.entity.Users;
import com.yoganavi.live_lecture.dto.LectureHistoryDto;
import com.yoganavi.live_lecture.repository.LectureScheduleRepository;
import com.yoganavi.live_lecture.repository.LiveLecturesRepository;
import com.yoganavi.live_lecture.repository.MyLiveLectureRepository;
import com.yoganavi.live_lecture.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HistoryServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HistoryServiceIntegrationTest.class);

    @Autowired
    private HistoryService historyService;

    @Autowired
    private LiveLecturesRepository liveLecturesRepository;

    @Autowired
    private MyLiveLectureRepository myLiveLectureRepository;

    @Autowired
    LectureScheduleRepository lectureScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    private static final LocalTime LECTURE_START_TIME = LocalTime.of(14, 0);
    private static final LocalTime LECTURE_END_TIME = LocalTime.of(16, 0);
    private static final LocalDate START_DATE = LocalDate.of(2024, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2024, 12, 31);


    @BeforeEach
    void setUp() {
        log.info("테스트 데이터 초기화 시작");

        myLiveLectureRepository.deleteAll();
        lectureScheduleRepository.deleteAll();
        liveLecturesRepository.deleteAll();
        userRepository.deleteAll();

        Users teacher = createUser(1L, "김강사");
        Users student = createUser(2L, "이학생");
        userRepository.saveAll(Arrays.asList(teacher, student));

        // 강의 생성
        LiveLectures lecture = createLecture(teacher.getUserId());
        List<LectureSchedule> schedules = createLectureSchedules(lecture);
        lecture.setSchedules(schedules);
        liveLecturesRepository.save(lecture);

        // 모든 강의를 수강 완료 상태로 설정
        schedules.forEach(schedule -> {
            MyLiveLecture myLecture = new MyLiveLecture();
            myLecture.setUserId(student.getUserId());
            myLecture.setLectureSchedule(schedule);
            myLecture.setCompleted(true);  // 수강 완료로 설정
            myLiveLectureRepository.save(myLecture);
        });
    }

    private Users createUser(Long userId, String nickname) {
        Users user = new Users();
        user.setUserId(userId);
        user.setNickname(nickname);
        user.setProfileImageUrl("http://test.com/" + nickname + ".jpg");
        user.setProfileImageUrlSmall("http://test.com/" + nickname + "_small.jpg");
        user.setIsActive(true);
        return user;
    }

    private LiveLectures createLecture(Long teacherId) {
        LiveLectures lecture = new LiveLectures();
        lecture.setLiveTitle("테스트 강의");
        lecture.setLiveContent("테스트 강의입니다");
        lecture.setMaxLiveNum(10);
        lecture.setUserId(teacherId);
        lecture.setRegDate(LocalDateTime.now());
        lecture.setIsOnAir(false);
        return lecture;
    }

    private List<LectureSchedule> createLectureSchedules(LiveLectures lecture) {
        List<LectureSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = START_DATE;

        while (!currentDate.isAfter(END_DATE)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.MONDAY ||
                dayOfWeek == DayOfWeek.WEDNESDAY ||
                dayOfWeek == DayOfWeek.FRIDAY) {

                LectureSchedule schedule = LectureSchedule.createSchedule(
                    currentDate,
                    LECTURE_START_TIME,
                    LECTURE_END_TIME
                );
                schedule.setLecture(lecture);
                schedules.add(schedule);
            }
            currentDate = currentDate.plusDays(1);
        }
        return schedules;
    }

    @Test
    void 수강_완료된_강의_조회() {
        // when
        List<LectureHistoryDto> result = historyService.getHistory(2L, 0, 20);

        // then
        assertThat(result).hasSize(6);

        // 첫 번째 강의 확인 (12월 13일)
        LectureHistoryDto firstLecture = result.get(0);
        assertThat(firstLecture.getLiveTitle()).isEqualTo("테스트 강의");
        assertThat(firstLecture.getNickname()).isEqualTo("김강사");

        LocalDateTime firstLectureTime = LocalDateTime.ofEpochSecond(
            firstLecture.getStartTime(), 0, ZoneOffset.UTC);
        assertThat(firstLectureTime.toLocalDate())
            .isEqualTo(LocalDate.of(2024, 12, 13));

        // 모든 강의 정보
        LocalDateTime previousDateTime = null;
        for (LectureHistoryDto lecture : result) {
            LocalDateTime lectureDateTime = LocalDateTime.ofEpochSecond(
                lecture.getStartTime(), 0, ZoneOffset.UTC);

            // 날짜가 범위 내에 있는지
            assertThat(lectureDateTime.toLocalDate())
                .isAfterOrEqualTo(START_DATE)
                .isBeforeOrEqualTo(END_DATE);

            // 시작/종료 시간 확인
            assertThat(lectureDateTime.toLocalTime()).isEqualTo(LECTURE_START_TIME);

            LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(
                lecture.getEndTime(), 0, ZoneOffset.UTC);
            assertThat(endDateTime.toLocalTime()).isEqualTo(LECTURE_END_TIME);

            // 요일 확인
            assertThat(lecture.getLectureDay())
                .isIn("MON", "WED", "FRI");

            // 시간 순서 확인 (내림차순 = 최신순)
            if (previousDateTime != null) {
                assertThat(lectureDateTime).isBefore(previousDateTime);
            }
            previousDateTime = lectureDateTime;
        }
    }

    @Test
    void 페이지네이션_동작_확인() {
        // when
        int pageSize = 5;
        List<LectureHistoryDto> firstPage = historyService.getHistory(2L, 0, pageSize);
        List<LectureHistoryDto> secondPage = historyService.getHistory(2L, 1, pageSize);

        // then
        assertThat(firstPage).hasSize(5);
        assertThat(secondPage).hasSize(1);

        // 시간 역순으로 정렬되었는지
        if (!secondPage.isEmpty()) {
            LocalDateTime lastFirstPage = LocalDateTime.ofEpochSecond(
                firstPage.get(firstPage.size() - 1).getStartTime(), 0, ZoneOffset.UTC);
            LocalDateTime firstSecondPage = LocalDateTime.ofEpochSecond(
                secondPage.get(0).getStartTime(), 0, ZoneOffset.UTC);

            assertThat(lastFirstPage).isAfter(firstSecondPage);
        }
    }


    @Test
    void 존재하지_않는_사용자_조회_시_예외_발생() {
        assertThatThrownBy(() -> historyService.getHistory(999L, 0, 10))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 수강_이력이_없는_경우_빈_리스트_반환() {
        // given
        Users newUser = createUser(4L, "신규사용자");
        userRepository.save(newUser);

        // when
        List<LectureHistoryDto> result = historyService.getHistory(4L, 0, 10);

        // then
        assertThat(result).isEmpty();
    }
}