package com.yoganavi.lecture.service.home;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.yoganavi.lecture.live_lecture.service.home.HomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.LiveLectures;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.live_lecture.dto.HomeResponseDto;
import com.yoganavi.lecture.live_lecture.repository.LectureScheduleRepository;
import com.yoganavi.lecture.live_lecture.repository.LiveLecturesRepository;
import com.yoganavi.lecture.live_lecture.repository.MyLiveLectureRepository;
import com.yoganavi.lecture.live_lecture.repository.UserRepository;
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
class HomeServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HomeServiceIntegrationTest.class);

    @Autowired
    private HomeService homeService;

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
    private static final LocalDate START_DATE = LocalDate.of(2024, 12, 8);
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
        log.info("사용자 생성 완료: 강사 ID={}, 학생 ID={}", teacher.getUserId(), student.getUserId());

        // 강의 메타데이터
        LiveLectures lecture = createLecture(teacher);

        // 강의 일정
        List<LectureSchedule> schedules = createLectureSchedules(lecture);
        log.info("생성된 강의 일정 수: {}", schedules.size());

        lecture.setSchedules(schedules);
        liveLecturesRepository.save(lecture);
        log.info("강의 저장 완료: ID={}, 제목={}", lecture.getLiveId(), lecture.getLiveTitle());

        // 학생 수강신청
        MyLiveLecture myLecture = new MyLiveLecture();
        myLecture.setUserId(student.getUserId());
        myLecture.setLectureSchedule(schedules.get(0));
        myLecture.setCompleted(false);
        myLiveLectureRepository.save(myLecture);
        log.info("수강신청 완료: 학생 ID={}, 스케줄 ID={}", student.getUserId(),
            schedules.get(0).getScheduleId());

        // 첫 번째 스케줄 정보
        LectureSchedule firstSchedule = schedules.get(0);
        log.info("첫 번째 강의 일정: 시작={}, 종료={}",
            firstSchedule.getStartTime(),
            firstSchedule.getEndTime());
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

    private LiveLectures createLecture(Users teacher) {
        LiveLectures lecture = new LiveLectures();
        lecture.setLiveTitle("테스트 강의");
        lecture.setLiveContent("테스트 강의입니다");
        lecture.setMaxLiveNum(10);
        lecture.setUser(teacher);
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
            currentDate = currentDate.plusDays(1);  // 날짜를 증가시켜야 함
        }
        return schedules;
    }

    @Test
    void 강사의_홈_화면_데이터_조회() {
        // when
        List<HomeResponseDto> result = homeService.getHomeData(1L, 0, 10);

        // then
        assertThat(result).isNotEmpty();
        HomeResponseDto firstLecture = result.get(0);

        assertThat(firstLecture.getLiveTitle()).isEqualTo("테스트 강의");
        assertThat(firstLecture.isTeacher()).isTrue();
        assertThat(firstLecture.getNickname()).isEqualTo("김강사");
    }

    @Test
    void 학생의_홈_화면_데이터_조회() {
        // when
        List<HomeResponseDto> result = homeService.getHomeData(2L, 0, 10);

        // then
        assertThat(result).isNotEmpty();
        HomeResponseDto firstLecture = result.get(0);

        assertThat(firstLecture.getLiveTitle()).isEqualTo("테스트 강의");
        assertThat(firstLecture.isTeacher()).isFalse();
        assertThat(firstLecture.getNickname()).isEqualTo("김강사");
    }

    @Test
    void 강의_일정이_정확한지_확인() {
        // when
        List<HomeResponseDto> result = homeService.getHomeData(1L, 0, 10);

        // then
        for (HomeResponseDto lecture : result) {
            LocalDateTime lectureDateTime = LocalDateTime.ofEpochSecond(
                lecture.getStartTime(), 0, ZoneOffset.UTC);

            // 날짜 범위 확인
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
        }

    }

    @Test
    void 페이지네이션_동작_확인() {
        // when
        int pageSize = 5;
        List<HomeResponseDto> firstPage = homeService.getHomeData(1L, 0, pageSize);
        List<HomeResponseDto> secondPage = homeService.getHomeData(1L, 1, pageSize);

        // then
        assertThat(firstPage).hasSize(pageSize);
        assertThat(secondPage).hasSizeLessThanOrEqualTo(pageSize);

        // 시간 순서대로 정렬되었는지 확인
        if (!secondPage.isEmpty()) {
            LocalDateTime lastFirstPage = LocalDateTime.ofEpochSecond(
                firstPage.get(firstPage.size() - 1).getStartTime(), 0, ZoneOffset.UTC);
            LocalDateTime firstSecondPage = LocalDateTime.ofEpochSecond(
                secondPage.get(0).getStartTime(), 0, ZoneOffset.UTC);

            assertThat(lastFirstPage).isBefore(firstSecondPage);
        }
    }

    @Test
    void 존재하지_않는_사용자_조회_시_예외_발생() {
        assertThatThrownBy(() -> homeService.getHomeData(999L, 0, 10))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 강사의_홈_화면_데이터_조회_복합() {
        // given
        // 두 번째 강사 생성
        Users anotherTeacher = createUser(3L, "박강사");
        userRepository.save(anotherTeacher);

        // 박강사 강의 생성 (12월 8일-31일 월/수/금)
        LiveLectures anotherLecture = createLecture(anotherTeacher);
        anotherLecture.setLiveTitle("요가 강의");
        List<LectureSchedule> anotherSchedules = createLectureSchedules(anotherLecture);
        anotherLecture.setSchedules(anotherSchedules);
        liveLecturesRepository.save(anotherLecture);

        // 김강사가 박강사의 강의를 수강신청
        MyLiveLecture teacherAsStudent = new MyLiveLecture();
        teacherAsStudent.setUserId(1L);  // 김강사
        teacherAsStudent.setLectureSchedule(anotherSchedules.get(0));
        teacherAsStudent.setCompleted(false);
        myLiveLectureRepository.save(teacherAsStudent);

        // when
        List<HomeResponseDto> result = homeService.getHomeData(1L, 0, 10);

        for (HomeResponseDto lecture : result) {
            System.out.println(lecture.toString());
        }

        // 나머지 검증
        for (HomeResponseDto lecture : result) {
            if (lecture.getLiveTitle().equals("테스트 강의")) {
                assertThat(lecture.isTeacher()).isTrue();
                assertThat(lecture.getNickname()).isEqualTo("김강사");
            } else {
                assertThat(lecture.getLiveTitle()).isEqualTo("요가 강의");
                assertThat(lecture.isTeacher()).isFalse();
                assertThat(lecture.getNickname()).isEqualTo("박강사");
            }

            // 날짜가 범위 내에 있는지
            LocalDateTime lectureDateTime = LocalDateTime.ofEpochSecond(
                lecture.getStartTime(), 0, ZoneOffset.UTC);
            assertThat(lectureDateTime.toLocalDate())
                .isAfterOrEqualTo(START_DATE)
                .isBeforeOrEqualTo(END_DATE);

            // 요일 확인
            assertThat(lecture.getLectureDay())
                .isIn("MON", "WED", "FRI");
        }

        // 두 번째 페이지
        List<HomeResponseDto> secondPage = homeService.getHomeData(1L, 1, 10);
        for (HomeResponseDto lecture : secondPage) {
            System.out.println(lecture.toString());
        }

    }

    @Test
    void 학생의_홈_화면_데이터_조회_여러강의_수강() {
        // given
        // 두 번째 강사 및 강의 생성
        Users anotherTeacher = createUser(3L, "박강사");
        userRepository.save(anotherTeacher);

        LiveLectures yogaLecture = createLecture(anotherTeacher);
        yogaLecture.setLiveTitle("요가 강의");
        List<LectureSchedule> yogaSchedules = createLectureSchedules(yogaLecture);
        yogaLecture.setSchedules(yogaSchedules);
        yogaLecture.setIsOnAir(true);  // 현재 방송중
        liveLecturesRepository.save(yogaLecture);

        // 학생이 요가 강의도 수강신청
        MyLiveLecture anotherEnrollment = new MyLiveLecture();
        anotherEnrollment.setUserId(2L);  // 이학생
        anotherEnrollment.setLectureSchedule(yogaSchedules.get(0));
        anotherEnrollment.setCompleted(false);
        myLiveLectureRepository.save(anotherEnrollment);

        // when - 첫 번째 페이지
        List<HomeResponseDto> firstPage = homeService.getHomeData(2L, 0, 10);

        for (HomeResponseDto lecture : firstPage) {
            System.out.println(lecture.toString());
        }

        // 시간순 정렬 확인
        for (int i = 0; i < firstPage.size() - 1; i++) {
            assertThat(firstPage.get(i).getStartTime())
                .isLessThanOrEqualTo(firstPage.get(i + 1).getStartTime());
        }

        // 모든 강의가 학생 입장인지
        for (HomeResponseDto lecture : firstPage) {
            assertThat(lecture.isTeacher()).isFalse();
            if (lecture.getLiveTitle().equals("요가 강의")) {
                assertThat(lecture.getNickname()).isEqualTo("박강사");
                assertThat(lecture.getIsOnAir()).isTrue();
            } else {
                assertThat(lecture.getLiveTitle()).isEqualTo("테스트 강의");
                assertThat(lecture.getNickname()).isEqualTo("김강사");
                assertThat(lecture.getIsOnAir()).isFalse();
            }
        }

        // 두 번째 페이지
        List<HomeResponseDto> secondPage = homeService.getHomeData(2L, 1, 10);
        for (HomeResponseDto lecture : secondPage) {
            System.out.println(lecture.toString());
        }

    }
}