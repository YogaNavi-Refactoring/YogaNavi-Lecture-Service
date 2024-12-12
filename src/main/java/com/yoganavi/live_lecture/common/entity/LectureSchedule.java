package com.yoganavi.live_lecture.common.entity;

import com.yoganavi.live_lecture.common.enums.LectureStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
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
    private LocalDate lectureDate; // 강의 날짜

    @Column(nullable = false)
    private LocalDateTime startTime; // 강의 시작 시간

    @Column(nullable = false)
    private LocalDateTime endTime;   // 강의 종료 시간

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;    // 강의 요일

    /**
     * 현재 강의가 진행 중인지
     *
     * @return 현재 시간이 강의 시간 내인 경우 true
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /**
     * 강의가 시작 전인지
     *
     * @return 현재 시간이 강의 시작 시간 이전인 경우 true
     */
    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startTime);
    }

    /**
     * 강의가 종료되었는지
     *
     * @return 현재 시간이 강의 종료 시간 이후인 경우 true
     */
    public boolean isCompleted() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * 강의 상태 확인
     *
     * @return 강의 현재 상태
     */
    public LectureStatus getStatus() {
        if (isUpcoming()) {
            return LectureStatus.UPCOMING;  // 들어야 함
        } else if (isActive()) {
            return LectureStatus.ACTIVE;    // 강의 듣는중
        } else {
            return LectureStatus.COMPLETED; // 수강 완료
        }
    }
}