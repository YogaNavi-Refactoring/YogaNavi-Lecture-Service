package com.yoganavi.lecture.common.entity;

import com.yoganavi.lecture.common.enums.LectureStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 개별 강의 일정 저장 특정 날짜와 시간에 진행되는 각각의 강의 관리
 */
@Entity
@Getter
@Setter
@Table(name = "lecture_schedules")
public class LectureSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_id", nullable = false)
    private LiveLectures lecture;

    @Column(nullable = false)
    private LocalDateTime startTime; // 강의 시작 시간

    @Column(nullable = false)
    private LocalDateTime endTime;   // 강의 종료 시간

    public LocalDate getLectureDate() {
        return startTime.toLocalDate();
    }

    public DayOfWeek getDayOfWeek() {
        return startTime.getDayOfWeek();
    }

    public static LectureSchedule createSchedule(LocalDate date, LocalTime startTime,
        LocalTime endTime) {
        LectureSchedule schedule = new LectureSchedule();
        schedule.startTime = date.atTime(startTime);
        schedule.endTime = date.atTime(endTime);
        return schedule;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startTime);
    }

    public boolean isCompleted() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public LectureStatus getStatus() {
        if (isUpcoming()) {
            return LectureStatus.UPCOMING;
        } else if (isActive()) {
            return LectureStatus.ACTIVE;
        } else {
            return LectureStatus.COMPLETED;
        }
    }

    /**
     * 스케줄 시간 차이 계산
     */
    public long getTimeDifference(LectureSchedule other) {
        return Math.abs(
            this.startTime.toLocalTime().toSecondOfDay() -
                other.startTime.toLocalTime().toSecondOfDay()
        );
    }

    /**
     * 같은 요일 여부 확인
     */
    public boolean isSameDay(LectureSchedule other) {
        return this.getDayOfWeek() == other.getDayOfWeek();
    }

    /**
     * 가장 가까운 스케줄 찾기
     */
    public static LectureSchedule findClosestSchedule(LectureSchedule oldSchedule,
        List<LectureSchedule> newSchedules) {
        LectureSchedule closestSchedule = null;
        long minTimeDiff = Long.MAX_VALUE;

        // 같은 요일 먼저 찾기
        for (LectureSchedule newSchedule : newSchedules) {
            if (oldSchedule.isSameDay(newSchedule)) {
                long timeDiff = oldSchedule.getTimeDifference(newSchedule);
                if (timeDiff < minTimeDiff) {
                    minTimeDiff = timeDiff;
                    closestSchedule = newSchedule;
                }
            }
        }

        // 같은 요일 없으면 시간대로만 찾기
        if (closestSchedule == null) {
            for (LectureSchedule newSchedule : newSchedules) {
                long timeDiff = oldSchedule.getTimeDifference(newSchedule);
                if (timeDiff < minTimeDiff) {
                    minTimeDiff = timeDiff;
                    closestSchedule = newSchedule;
                }
            }
        }

        if (closestSchedule == null) {
            throw new IllegalStateException("새로운 스케줄이 없습니다.");
        }

        return closestSchedule;
    }
}