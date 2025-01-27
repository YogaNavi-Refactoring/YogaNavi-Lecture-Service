package com.yoganavi.lecture.reserve.service;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.LiveLectures;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.LiveLecturesRepository;
import com.yoganavi.lecture.common.repository.MyLiveLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.reserve.dto.LiveLectureDto;
import com.yoganavi.lecture.reserve.dto.ReservationInfoDto;
import com.yoganavi.lecture.reserve.dto.ReservationRequestDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final LiveLecturesRepository liveLecturesRepository;
    private final MyLiveLectureRepository myLiveLectureRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createReservation(long userId, ReservationRequestDto request) {
        validateRequest(request);

        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        LiveLectures lecture = liveLecturesRepository.findByIdWithUserAndSchedules(request.getLiveId())
            .orElseThrow(() -> new EntityNotFoundException("강의를 찾을 수 없습니다."));

        validateReservation(lecture, user);

        // 시간 중복 검증
        LocalDateTime startDateTime = LocalDateTime.ofInstant(request.getStartDate(), ZoneId.of("Asia/Seoul"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(request.getEndDate(), ZoneId.of("Asia/Seoul"));

        List<MyLiveLecture> overlappingLectures = myLiveLectureRepository.findOverlappingLectures(
            user.getUserId(), startDateTime, endDateTime);
        if (!overlappingLectures.isEmpty()) {
            throw new IllegalArgumentException("해당 시간에 이미 수강중인 강의가 있습니다.");
        }

        // 예약 가능한 스케줄 확인
        List<LectureSchedule> schedulesToReserve = getSchedulesInPeriod(lecture, request);
        if (schedulesToReserve.isEmpty()) {
            throw new IllegalArgumentException("예약 가능한 스케줄이 없습니다.");
        }

        createReservations(user, schedulesToReserve);
    }

    private List<LectureSchedule> getSchedulesInPeriod(LiveLectures lecture,
        ReservationRequestDto request) {
        LocalDateTime startDateTime = LocalDateTime.ofInstant(request.getStartDate(),
            ZoneId.of("Asia/Seoul"));
        LocalDateTime endDateTime = LocalDateTime.ofInstant(request.getEndDate(),
            ZoneId.of("Asia/Seoul"));

        if (startDateTime.isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
            throw new IllegalArgumentException("과거 날짜는 예약할 수 없습니다.");
        }

        return lecture.getSchedules().stream()
            .filter(schedule -> isWithinPeriod(schedule, startDateTime, endDateTime)
                && schedule.isUpcoming())
            .collect(Collectors.toList());
    }

    private void validateRequest(ReservationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("예약 정보가 없습니다.");
        }
        if (request.getLiveId() == null) {
            throw new IllegalArgumentException("강의 ID가 없습니다.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("예약 기간이 올바르지 않습니다.");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.");
        }
    }

    private void createReservations(Users user, List<LectureSchedule> schedules) {
        try {
            for (LectureSchedule schedule : schedules) {
                MyLiveLecture reservation = new MyLiveLecture();
                reservation.setUser(user);
                reservation.setLectureSchedule(schedule);
                myLiveLectureRepository.save(reservation);
            }
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 예약된 강의입니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("예약 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationInfoDto> getLiveLecturesByTeacherAndMethod(long teacherId, int method) {
        if (method != 0 && method != 1) {
            throw new IllegalArgumentException("올바르지 않은 조회 방법입니다.");
        }

        List<LiveLectures> lectures = Optional.ofNullable(
                liveLecturesRepository.findByUserId(teacherId))
            .orElse(Collections.emptyList());

        List<ReservationInfoDto> result = new ArrayList<>();
        for (LiveLectures lecture : lectures) {
            if (isLectureAvailable(lecture, method)) {
                result.add(convertToReservationInfo(lecture));
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LiveLectureDto> getAllLiveLectures(int method) {
        List<LiveLectures> lectures = liveLecturesRepository.findAvailableLectures(method,
            LocalDateTime.now());
        List<LiveLectureDto> result = new ArrayList<>();

        for (LiveLectures lecture : lectures) {
            long currentParticipants = myLiveLectureRepository.countByLiveLectureId(
                lecture.getLiveId());
            if (currentParticipants >= lecture.getMaxLiveNum()) {
                continue;
            }

            result.add(convertToDto(lecture));
        }

        return result;
    }

    private LiveLectureDto convertToDto(LiveLectures lecture) {
        LiveLectureDto dto = new LiveLectureDto();
        dto.setLiveId(lecture.getLiveId());
        dto.setLiveTitle(lecture.getLiveTitle());
        dto.setLiveContent(lecture.getLiveContent());
        dto.setMaxLiveNum(lecture.getMaxLiveNum());
        dto.setAvailableDay(lecture.getAvailableDay());
        dto.setRegDate(lecture.getRegDate().atZone(ZoneId.systemDefault()).toInstant());
        dto.setUserId(lecture.getUser().getUserId().intValue());

        LocalDateTime firstStart = lecture.getFirstStartTime();
        LocalDateTime lastEnd = lecture.getLastEndTime();

        if (firstStart != null) {
            dto.setStartDate(firstStart.atZone(ZoneId.systemDefault()).toInstant());
            dto.setStartTime(firstStart.atZone(ZoneId.systemDefault()).toInstant());
        }

        if (lastEnd != null) {
            dto.setEndDate(lastEnd.atZone(ZoneId.systemDefault()).toInstant());
            dto.setEndTime(lastEnd.atZone(ZoneId.systemDefault()).toInstant());
        }

        return dto;
    }

    private boolean isLectureAvailable(LiveLectures lecture, int method) {
        return !lecture.getIsDeleted() &&
            lecture.hasUpcomingSchedules() &&
            hasAvailableCapacity(lecture) &&
            ((method == 0 && lecture.getMaxLiveNum() == 1) ||
                (method == 1 && lecture.getMaxLiveNum() > 1));
    }

    private void validateReservation(LiveLectures lecture, Users user) {
        if (lecture.getIsDeleted()) {
            throw new IllegalStateException("삭제된 강의입니다.");
        }

        if (lecture.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("자신의 강의는 수강신청할 수 없습니다.");
        }

        if (!lecture.hasUpcomingSchedules()) {
            throw new IllegalStateException("예약 가능한 일정이 없습니다.");
        }

        if (!hasAvailableCapacity(lecture)) {
            throw new IllegalStateException("수강 인원이 초과되었습니다.");
        }
    }

    private boolean hasAvailableCapacity(LiveLectures lecture) {
        long currentStudents = myLiveLectureRepository.countByLiveLectureId(lecture.getLiveId());
        return currentStudents < lecture.getMaxLiveNum();
    }

    private boolean isWithinPeriod(LectureSchedule schedule,
        LocalDateTime startDate, LocalDateTime endDate) {
        return !schedule.getStartTime().isBefore(startDate) &&
            !schedule.getStartTime().isAfter(endDate);
    }

    private void validateScheduleOverlap(Users user, List<LectureSchedule> newSchedules) {
        for (LectureSchedule newSchedule : newSchedules) {
            List<MyLiveLecture> existingLectures = myLiveLectureRepository
                .findOverlappingLectures(
                    user.getUserId(),
                    newSchedule.getStartTime(),
                    newSchedule.getEndTime()
                );

            if (!existingLectures.isEmpty()) {
                throw new IllegalArgumentException("해당 시간에 이미 수강중인 강의가 있습니다.");
            }
        }
    }

    private boolean isTimeOverlap(LectureSchedule schedule1, LectureSchedule schedule2) {
        return !schedule1.getEndTime().isBefore(schedule2.getStartTime()) &&
            !schedule2.getEndTime().isBefore(schedule1.getStartTime());
    }

    private ReservationInfoDto convertToReservationInfo(LiveLectures lecture) {
        return ReservationInfoDto.builder()
            .liveId(lecture.getLiveId())
            .liveTitle(lecture.getLiveTitle())
            .startDate(lecture.getFirstStartTime())
            .endDate(lecture.getLastEndTime())
            .availableDay(lecture.getAvailableDay())
            .build();
    }
}