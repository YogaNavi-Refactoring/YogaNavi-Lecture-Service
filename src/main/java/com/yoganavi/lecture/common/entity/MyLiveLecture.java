package com.yoganavi.lecture.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 학생별 수강 강의 정보 저장 각 강의 일정(LectureSchedule)에 대한 수강 상태 관리
 */
@Entity
@Getter
@Setter
@Table(name = "my_live_lectures",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "schedule_id"})
    })
public class MyLiveLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long myListId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private LectureSchedule lectureSchedule;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean completed = false;

    /**
     * 현재 수강 가능한 강의인지
     *
     * @return 강의가 끝나지 않음, 수강 완료하지 않은 경우 true
     */
    public boolean isAvailable() {
        return !completed && !lectureSchedule.isCompleted();
    }

    /**
     * 현재 진행중인 강의인지
     *
     * @return 수강 완료하지 않음, 현재 강의가 진행중인 경우 true
     */
    public boolean isInProgress() {
        return !completed && lectureSchedule.isActive();
    }
}