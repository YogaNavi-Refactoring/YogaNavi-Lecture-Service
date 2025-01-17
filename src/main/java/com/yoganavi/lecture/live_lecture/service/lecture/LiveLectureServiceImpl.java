package com.yoganavi.lecture.live_lecture.service.lecture;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.LiveLectures;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.util.TimeUtil;
import com.yoganavi.lecture.live_lecture.dto.LiveLectureInfoDto;
import com.yoganavi.lecture.live_lecture.dto.LiveLectureResponseDto;
import com.yoganavi.lecture.common.repository.LiveLecturesRepository;
import com.yoganavi.lecture.common.repository.MyLiveLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveLectureServiceImpl implements LiveLectureService {

    private final TimeUtil timeUtil;
    private final UserRepository userRepository;
    private final LiveLecturesRepository liveLecturesRepository;
    private final MyLiveLectureRepository myLiveLectureRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createLiveLecture(LiveLectureInfoDto dto) {
        log.info("라이브 강의 생성 시작: 사용자 ID {}", dto.getUserId());

        if (dto.getUserId() == 0) {
            log.error("사용자 ID는 0일 수 없습니다");
            throw new IllegalArgumentException("사용자 ID는 0일 수 없습니다");
        }

        Users user = userRepository.findById(dto.getUserId()).orElseThrow(
            () -> new IllegalArgumentException("사용자" + dto.getUserId() + "를 찾을 수 없습니다."));

        LiveLectures liveLecture = new LiveLectures();
        liveLecture.setLiveTitle(dto.getLiveTitle());
        liveLecture.setLiveContent(dto.getLiveContent());
        liveLecture.setMaxLiveNum(dto.getMaxLiveNum());
        liveLecture.setRegDate(LocalDateTime.now());
        liveLecture.setUser(user);
        liveLecture.setIsOnAir(false);

        validateScheduleUpdate(dto);
        validateAvailableDay(dto.getAvailableDay());

        LocalDate startDate = timeUtil.toLocalDate(dto.getStartDate());
        LocalDate endDate = timeUtil.toLocalDate(dto.getEndDate());
        LocalTime startTime = timeUtil.toLocalTime(dto.getStartTime());
        LocalTime endTime = timeUtil.toLocalTime(dto.getEndTime());

        // 강의 기간 동안의 모든 일정 생성
        List<LectureSchedule> schedules = generateSchedules(
            startDate,
            endDate,
            startTime,
            endTime,
            dto.getAvailableDay()
        );

        schedules.forEach(schedule -> {
            schedule.setLecture(liveLecture);
            liveLecture.getSchedules().add(schedule);
        });

        LiveLectures savedLecture = liveLecturesRepository.save(liveLecture);
        log.info("라이브 강의 저장 완료: 강의 ID {}", savedLecture.getLiveId());

//        notificationService.handleLectureUpdate(savedLecture);

    }

    private List<LectureSchedule> generateSchedules(
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String availableDays
    ) {
        List<LectureSchedule> schedules = new ArrayList<>();
        Set<DayOfWeek> availableDaySet = convertStrToDayOfWeek(availableDays);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (availableDaySet.contains(currentDate.getDayOfWeek())) {
                LectureSchedule schedule = LectureSchedule.createSchedule(
                    currentDate,
                    startTime,
                    endTime
                );
                schedules.add(schedule);
            }
            currentDate = currentDate.plusDays(1);
        }

        return schedules;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveLectureResponseDto> getLiveLecturesByUserId(Long userId) {
        log.info("사용자의 라이브 강의 목록 조회 시작: 사용자 ID {}", userId);
        List<LiveLectureResponseDto> responseDtos = new ArrayList<>();

        try {
            List<LiveLectures> lectures = liveLecturesRepository.findByUserId(userId);

            for (LiveLectures lecture : lectures) {
                responseDtos.add(convertToDto(lecture));
            }
        } catch (Exception e) {
            log.error("사용자의 라이브 강의 목록 조회 실패: 사용자 ID {}", userId);
        }
        log.info("사용자의 라이브 강의 목록 조회 완료: 사용자 ID {}, 조회된 강의 수 {}",
            userId, responseDtos.size());

        return responseDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLectureOwner(Long liveId, Long userId) {
        Optional<LiveLectures> lectureOpt = Optional.empty();
        try {
            log.info("사용자 ID {}에 대한 강의 ID {}의 강사 여부 판별", userId, liveId);
            lectureOpt = liveLecturesRepository.findById(liveId);
        } catch (Exception e) {
            log.error("사용자 ID {}에 대한 강의 ID {}의 강사 여부 판별 실패", userId, liveId);
        }
        return lectureOpt.filter(lecture ->
                Objects.equals(lecture.getUser().getUserId(), userId))
            .isPresent();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateLiveLecture(LiveLectureInfoDto liveLectureInfoDto) {
        log.info("라이브 강의 수정 시작: 강의 ID {}", liveLectureInfoDto.getLiveId());

        LiveLectures lecture = liveLecturesRepository.findById(liveLectureInfoDto.getLiveId())
            .orElseThrow(() -> new IllegalArgumentException(
                "강의를 찾을 수 없습니다: " + liveLectureInfoDto.getLiveId()));

        if (lecture.getIsDeleted()) {
            log.error("이미 삭제된 강의는 수정할 수 없습니다");
            throw new IllegalStateException("이미 삭제된 강의는 수정할 수 없습니다");
        }

        if (lecture.getIsOnAir()) {
            log.error("진행 중인 강의는 수정할 수 없습니다");
            throw new IllegalStateException("진행 중인 강의는 수정할 수 없습니다");
        }

        if (liveLectureInfoDto.getLiveTitle() != null && liveLectureInfoDto.getLiveTitle().trim()
            .isEmpty()) {
            log.error("강의 제목은 비워둘 수 없습니다");
            throw new IllegalArgumentException("강의 제목은 비워둘 수 없습니다");
        }

        if (liveLectureInfoDto.getMaxLiveNum() != null && liveLectureInfoDto.getMaxLiveNum() <= 0) {
            log.error("최대 수강 인원은 1명 이상이어야 합니다");
            throw new IllegalArgumentException("최대 수강 인원은 1명 이상이어야 합니다");
        }

        updateBasicLectureInfo(lecture, liveLectureInfoDto);

        if (isScheduleUpdateRequired(liveLectureInfoDto)) {
            validateScheduleUpdate(liveLectureInfoDto);
            if (liveLectureInfoDto.getAvailableDay() != null) {
                validateAvailableDay(liveLectureInfoDto.getAvailableDay());
            }
            updateLectureSchedules(lecture, liveLectureInfoDto);
        }

        LiveLectures updatedLecture = liveLecturesRepository.save(lecture);
        log.info("라이브 강의 수정 완료: 강의 ID {}", updatedLecture.getLiveId());

//        notificationService.handleLectureUpdate(updatedLecture);
//        notificationService.sendLectureUpdateNotification(updatedLecture);
    }

    private void validateScheduleUpdate(LiveLectureInfoDto dto) {
        LocalDate startDate = timeUtil.toLocalDate(dto.getStartDate());
        LocalDate endDate = timeUtil.toLocalDate(dto.getEndDate());
        LocalTime startTime = timeUtil.toLocalTime(dto.getStartTime());
        LocalTime endTime = timeUtil.toLocalTime(dto.getEndTime());

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("종료일이 시작일보다 빠를 수 없습니다");
            }
            if (startDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("시작일은 오늘 이후여야 합니다");
            }
        }

        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("종료 시간이 시작 시간보다 빠를 수 없습니다");
        }
    }

    private boolean isScheduleUpdateRequired(LiveLectureInfoDto dto) {
        return dto.getStartDate() != null || dto.getEndDate() != null ||
            dto.getStartTime() != null || dto.getEndTime() != null ||
            dto.getAvailableDay() != null;
    }

    private void updateLectureSchedules(LiveLectures lecture, LiveLectureInfoDto dto) {
        // 스케줄을 과거/미래로 분류하고 수강생 정보 저장
        Map<String, List<LectureSchedule>> scheduleMap = lecture.divideSchedulesByTime();
        List<LectureSchedule> futureSchedules = scheduleMap.get("future");

        // 미래 스케줄의 수강생 정보 저장
        Map<LectureSchedule, List<MyLiveLecture>> scheduleEnrollments = new HashMap<>();
        for (LectureSchedule schedule : futureSchedules) {
            List<MyLiveLecture> enrollments = myLiveLectureRepository.findAllByLectureSchedule(
                schedule);
            if (!enrollments.isEmpty()) {
                scheduleEnrollments.put(schedule, enrollments);
            }
        }

        // 미래 스케줄만 제거
        lecture.clearFutureSchedules();

        // 새로운 스케줄 생성 및 추가
        List<LectureSchedule> newSchedules = createNewSchedules(lecture, dto);

        // 수강생 재배치
        if (!scheduleEnrollments.isEmpty()) {
            redistributeStudents(scheduleEnrollments, newSchedules);
        }
    }

    private List<LectureSchedule> createNewSchedules(LiveLectures lecture, LiveLectureInfoDto dto) {
        List<LectureSchedule> newSchedules = new ArrayList<>();
        LocalDate startDate = timeUtil.toLocalDate(dto.getStartDate());
        LocalDate endDate = timeUtil.toLocalDate(dto.getEndDate());
        LocalTime startTime = timeUtil.toLocalTime(dto.getStartTime());
        LocalTime endTime = timeUtil.toLocalTime(dto.getEndTime());

        Set<DayOfWeek> availableDays = convertStrToDayOfWeek(dto.getAvailableDay());
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (availableDays.contains(currentDate.getDayOfWeek())) {
                LocalDateTime scheduleDateTime = currentDate.atTime(startTime);
                if (scheduleDateTime.isAfter(now)) {
                    LectureSchedule schedule = LectureSchedule.createSchedule(
                        currentDate,
                        startTime,
                        endTime
                    );
                    schedule.setLecture(lecture);
                    lecture.getSchedules().add(schedule);
                    newSchedules.add(schedule);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        return newSchedules;
    }

    public void redistributeStudents(Map<LectureSchedule, List<MyLiveLecture>> scheduleEnrollments,
        List<LectureSchedule> newSchedules) {
        for (Entry<LectureSchedule, List<MyLiveLecture>> entry : scheduleEnrollments.entrySet()) {
            LectureSchedule oldSchedule = entry.getKey();
            List<MyLiveLecture> students = entry.getValue();

            LectureSchedule closestSchedule = LectureSchedule.findClosestSchedule(oldSchedule,
                newSchedules);

            for (MyLiveLecture enrollment : students) {
                MyLiveLecture newEnrollment = new MyLiveLecture();
                newEnrollment.setLectureSchedule(closestSchedule);
                newEnrollment.setUser(enrollment.getUser());
                newEnrollment.setCompleted(false);

                myLiveLectureRepository.save(newEnrollment);

                // 수강생에게 스케줄 변경 알림
//                notificationService.sendScheduleChangeNotification(
//                    enrollment.getUserId(),
//                    oldSchedule,
//                    closestSchedule
//                );
            }
        }
    }

    private Set<DayOfWeek> convertStrToDayOfWeek(String dayCodesStr) {
        if (dayCodesStr == null || dayCodesStr.trim().isEmpty()) {
            return new HashSet<>();
        }

        Map<String, DayOfWeek> dayCodeMap = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY
        );

        Set<DayOfWeek> availableDays = new HashSet<>();
        String[] dayCodes = dayCodesStr.split(",");

        for (String dayCode : dayCodes) {
            String trimmedDayCode = dayCode.trim();
            DayOfWeek dayOfWeek = dayCodeMap.get(trimmedDayCode);
            if (dayOfWeek == null) {
                log.warn("잘못된 요일 코드: {}", trimmedDayCode);
                throw new IllegalArgumentException("잘못된 요일 코드입니다: " + trimmedDayCode);
            }
            availableDays.add(dayOfWeek);
        }

        return availableDays;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteLiveLectureById(Long liveId) {
        log.info("라이브 강의 삭제 시작: 강의 ID {}", liveId);

        LiveLectures lecture = findLecture(liveId);

        if (lecture.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 강의입니다.");
        }

        if (hasActiveSchedule(lecture)) {
            throw new IllegalStateException("진행 중인 강의는 삭제할 수 없습니다.");
        }

        List<MyLiveLecture> lectureStudents = myLiveLectureRepository.findByLiveLectureLiveId(
            liveId);

        if (lectureStudents.isEmpty()) {
            processCompleteDelete(lecture);
        } else {
            processFutureScheduleDelete(lecture, lectureStudents);
        }
    }

    private LiveLectures findLecture(Long liveId) {
        return liveLecturesRepository.findById(liveId)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 강의 ID: " + liveId));
    }

    private boolean hasActiveSchedule(LiveLectures lecture) {
        if (lecture.getIsOnAir()) {
            return true;
        }
        return lecture.hasActiveSchedules();
    }

    private void processCompleteDelete(LiveLectures lecture) {
        lecture.getSchedules().clear();
        softDeleteLecture(lecture);

        log.info("강의 및 전체 스케줄 삭제 완료: 강의 ID {}", lecture.getLiveId());
    }

    private void processFutureScheduleDelete(LiveLectures lecture,
        List<MyLiveLecture> enrolledStudents) {
        List<LectureSchedule> scheduleToKeep = new ArrayList<>();
        List<LectureSchedule> scheduleToDelete = new ArrayList<>();

        for (LectureSchedule schedule : lecture.getSchedules()) {
            if (schedule.isCompleted()) {
                scheduleToKeep.add(schedule);
            } else {
                scheduleToDelete.add(schedule);
            }
        }

        lecture.getSchedules().clear();
        lecture.getSchedules().addAll(scheduleToKeep);

        softDeleteLecture(lecture);

        log.info("미래 스케줄 삭제 완료: 강의 ID {}, 삭제된 스케줄 수: {}, 유지된 스케줄 수: {}",
            lecture.getLiveId(), scheduleToDelete.size(), scheduleToKeep.size());

        if (!scheduleToDelete.isEmpty()) {
//            notificationService.sendLectureScheduleDeletionNotification(lecture, enrolledStudents);
        }
    }

    private void softDeleteLecture(LiveLectures lecture) {
        lecture.setIsDeleted(true);
        lecture.setDeletedAt(LocalDateTime.now());
        liveLecturesRepository.save(lecture);
    }

    @Transactional(readOnly = true)
    public LiveLectureResponseDto getLiveLectureById(Long liveId) {
        log.info("단일 라이브 강의 조회: 강의 ID {}", liveId);

        LiveLectures lecture = liveLecturesRepository.findByIdWithUserAndSchedules(liveId)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 강의 ID: " + liveId));

        return convertToDto(lecture);
    }

    private LiveLectureResponseDto convertToDto(LiveLectures lecture) {
        LiveLectureResponseDto dto = new LiveLectureResponseDto();

        // 강의 기본 정보
        dto.setLiveId(lecture.getLiveId());
        dto.setLiveTitle(lecture.getLiveTitle());
        dto.setLiveContent(lecture.getLiveContent());
        dto.setMaxLiveNum(lecture.getMaxLiveNum());
        dto.setRegDate(timeUtil.toEpochMilli(lecture.getRegDate()));

        // 강의 기간 (첫 스케줄 ~ 마지막 스케줄)
        LocalDateTime firstSchedule = lecture.getFirstStartTime();
        if (firstSchedule != null) {
            dto.setStartDate(timeUtil.toEpochMilli(firstSchedule));
        }

        LocalDateTime lastSchedule = lecture.getLastEndTime();
        if (lastSchedule != null) {
            dto.setEndDate(timeUtil.toEpochMilli(lastSchedule));
        }

        // 강의 시간 설정
        LocalTime startTime = lecture.getLectureStartTime();
        if (startTime != null) {
            dto.setStartTime(timeUtil.timeToEpochMilli(startTime));
        }

        LocalTime endTime = lecture.getLectureEndTime();
        if (endTime != null) {
            dto.setEndTime(timeUtil.timeToEpochMilli(endTime));
        }

        // 강사 정보
        dto.setUserId(lecture.getUser().getUserId().intValue());
        dto.setNickname(lecture.getUser().getNickname());
        dto.setProfileImageUrl(lecture.getUser().getProfileImageUrl());
        dto.setProfileImageUrlSmall(lecture.getUser().getProfileImageUrlSmall());

        // 강의 요일 정보
        dto.setAvailableDay(lecture.getAvailableDay());

        return dto;
    }

    private void validateAvailableDay(String availableDays) {
        if (availableDays == null || availableDays.trim().isEmpty()) {
            throw new IllegalArgumentException("강의 요일을 선택해야 합니다");
        }

        Map<String, DayOfWeek> dayCodeMap = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY
        );

        String[] days = availableDays.split(",");
        for (String day : days) {
            String trimmedDay = day.trim();
            if (!dayCodeMap.containsKey(trimmedDay)) {
                throw new IllegalArgumentException("잘못된 요일 코드입니다: " + trimmedDay);
            }
        }
    }

    private void updateBasicLectureInfo(LiveLectures lecture, LiveLectureInfoDto dto) {
        if (dto.getLiveTitle() != null) {
            lecture.setLiveTitle(dto.getLiveTitle());
        }
        if (dto.getLiveContent() != null) {
            lecture.setLiveContent(dto.getLiveContent());
        }
        if (dto.getMaxLiveNum() != null) {
            lecture.setMaxLiveNum(dto.getMaxLiveNum());
        }
    }

}
