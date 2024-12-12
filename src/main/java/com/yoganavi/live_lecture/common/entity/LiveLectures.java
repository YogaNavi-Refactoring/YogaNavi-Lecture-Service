package com.yoganavi.live_lecture.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(nullable = false)
    private Long userId;    // 강사 ID

    @Column(nullable = false)
    private Boolean isOnAir = false;    // 강의 진행 상태 (true: 진행 중, false: 종료)

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureSchedule> schedules = new ArrayList<>();

}