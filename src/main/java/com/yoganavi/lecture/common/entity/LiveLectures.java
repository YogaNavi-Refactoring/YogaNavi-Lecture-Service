package com.yoganavi.lecture.common.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * 강의 메타데이터를 저장 강의의 기본 정보, 개별 강의 일정(LectureSchedule) 관리
 */
@Entity
@Getter
@Setter
@Table(name = "live_lectures")
public class LiveLectures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long liveId;    // 강의 고유 식별자

    @Column(length = 30, nullable = false)
    private String liveTitle;   // 강의 제목

    @Column(length = 300)
    private String liveContent; // 강의 설명

    @Column(nullable = false)
    private Integer maxLiveNum; // 최대 수강 인원

    @Column(nullable = false)
    private LocalDateTime regDate;  // 강의 등록 일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;    // 강사 정보

    @Column(nullable = false)
    private Boolean isOnAir = false;    // 강의 진행 상태 (true: 진행 중, false: 종료)

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureSchedule> schedules = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    public void validateModifiable() {
        if (Boolean.TRUE.equals(this.isDeleted)) {
            throw new IllegalStateException("삭제된 강의는 수정할 수 없습니다.");
        }
    }

    /**
     * 진행 완료된 스케줄이 있는지 확인
     */
    public boolean hasCompletedSchedules() {
        for (LectureSchedule schedule : this.schedules) {
            if (schedule.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 진행 예정인 스케줄이 있는지 확인
     */
    public boolean hasUpcomingSchedules() {
        for (LectureSchedule schedule : this.schedules) {
            if (schedule.isUpcoming()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 진행중인 스케줄이 있는지 확인
     */
    public boolean hasActiveSchedules() {
        for (LectureSchedule schedule : this.schedules) {
            if (schedule.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 첫 시작 시간 반환
     */
    public LocalDateTime getFirstStartTime() {
        if (schedules.isEmpty()) {
            return null;
        }

        LocalDateTime firstStart = null;
        for (LectureSchedule schedule : schedules) {
            if (firstStart == null || schedule.getStartTime().isBefore(firstStart)) {
                firstStart = schedule.getStartTime();
            }
        }
        return firstStart;
    }

    /**
     * 마지막 종료 시간 반환
     */
    public LocalDateTime getLastEndTime() {
        if (schedules.isEmpty()) {
            return null;
        }

        LocalDateTime lastEnd = null;
        for (LectureSchedule schedule : schedules) {
            if (lastEnd == null || schedule.getEndTime().isAfter(lastEnd)) {
                lastEnd = schedule.getEndTime();
            }
        }
        return lastEnd;
    }

    /**
     * 강의 시작 시간
     */
    public LocalTime getLectureStartTime() {
        LocalDateTime startTime = getFirstStartTime();
        if (startTime == null) {
            return null;
        }
        return startTime.toLocalTime();
    }

    /**
     * 강의 종료 시간
     */
    public LocalTime getLectureEndTime() {
        LocalDateTime endTime = getLastEndTime();
        if (endTime == null) {
            return null;
        }
        return endTime.toLocalTime();
    }

    /**
     * 스케줄 요일 정보 반환
     */
    public String getAvailableDay() {
        if (schedules.isEmpty()) {
            return "";
        }

        Set<String> uniqueDays = new HashSet<>();
        StringBuilder days = new StringBuilder();

        for (LectureSchedule schedule : schedules) {
            String dayCode = convertDayOfWeekToCode(schedule.getDayOfWeek());
            if (!uniqueDays.contains(dayCode)) {
                if (days.length() > 0) {
                    days.append(",");
                }
                days.append(dayCode);
                uniqueDays.add(dayCode);
            }
        }

        return days.toString();
    }

    private String convertDayOfWeekToCode(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "MON";
            case TUESDAY:
                return "TUE";
            case WEDNESDAY:
                return "WED";
            case THURSDAY:
                return "THU";
            case FRIDAY:
                return "FRI";
            case SATURDAY:
                return "SAT";
            case SUNDAY:
                return "SUN";
            default:
                throw new IllegalArgumentException("유효하지 않은 요일: " + dayOfWeek);
        }
    }

    /**
     * 스케줄을 과거/미래로 분류
     */
    public Map<String, List<LectureSchedule>> divideSchedulesByTime() {
        Map<String, List<LectureSchedule>> result = new HashMap<>();
        List<LectureSchedule> pastSchedules = new ArrayList<>();
        List<LectureSchedule> futureSchedules = new ArrayList<>();

        for (LectureSchedule schedule : this.schedules) {
            if (schedule.isCompleted()) {
                pastSchedules.add(schedule);
            } else {
                futureSchedules.add(schedule);
            }
        }

        result.put("past", pastSchedules);
        result.put("future", futureSchedules);
        return result;
    }

    /**
     * 미래 스케줄만 제거
     */
    public void clearFutureSchedules() {
        this.schedules.removeIf(schedule -> !schedule.isCompleted());
    }
}
