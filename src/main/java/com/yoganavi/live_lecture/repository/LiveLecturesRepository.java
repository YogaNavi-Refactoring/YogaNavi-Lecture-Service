package com.yoganavi.live_lecture.repository;

import com.yoganavi.live_lecture.common.entity.LiveLectures;
import com.yoganavi.live_lecture.dto.HomeResponseDto;
import com.yoganavi.live_lecture.dto.LectureHistoryDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveLecturesRepository extends JpaRepository<LiveLectures, Long> {

    @Query("""
        SELECT new com.yoganavi.live_lecture.dto.HomeResponseDto(
            l.liveId,
            t.nickname,
            t.profileImageUrl,
            t.profileImageUrlSmall,
            l.liveTitle,
            l.liveContent,
            s.startTime,
            s.endTime,
            cast(function('date_format', s.startTime, '%a') as string),
            l.maxLiveNum,
            cast(CASE WHEN l.userId = :userId THEN true ELSE false END as boolean),
            l.isOnAir 
        )
        FROM LiveLectures l
        JOIN Users t ON t.userId = l.userId
        JOIN l.schedules s
        WHERE (l.userId = :userId
              OR EXISTS (
                  SELECT 1 FROM MyLiveLecture ml 
                  WHERE ml.lectureSchedule.lecture = l 
                  AND ml.userId = :userId 
                  AND ml.completed = false
              ))
        AND s.endTime > :now
        ORDER BY s.startTime ASC
        """)
    List<HomeResponseDto> findAllMyLectures(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    @Query("""
        SELECT new com.yoganavi.live_lecture.dto.LectureHistoryDto(
            l.liveId,
            t.nickname,
            t.profileImageUrlSmall,
            l.liveTitle,
            s.startTime,
            s.endTime,
            cast(function('date_format', s.startTime, '%a') as string)
        )
        FROM LiveLectures l
        JOIN Users t ON t.userId = l.userId
        JOIN l.schedules s
        WHERE (l.userId = :userId
              OR EXISTS (
                  SELECT 1 FROM MyLiveLecture ml 
                  WHERE ml.lectureSchedule.lecture = l 
                  AND ml.userId = :userId 
                  AND ml.completed = true  
              ))
        AND s.endTime <= :now
        ORDER BY s.startTime DESC
        """)
    List<LectureHistoryDto> findCompletedLectures(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
}
