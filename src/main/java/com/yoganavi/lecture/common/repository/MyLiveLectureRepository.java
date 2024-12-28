package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import com.yoganavi.lecture.common.entity.MyLiveLecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MyLiveLectureRepository extends JpaRepository<MyLiveLecture, Long> {

    @Query("SELECT ml FROM MyLiveLecture ml " +
        "JOIN FETCH ml.lectureSchedule ls " +
        "WHERE ls = :lectureSchedule")
    List<MyLiveLecture> findAllByLectureSchedule(
        @Param("lectureSchedule") LectureSchedule lectureSchedule);

    // 수강 완료하지 않은 수강생만
    @Query("SELECT ml FROM MyLiveLecture ml " +
        "WHERE ml.lectureSchedule = :lectureSchedule " +
        "AND ml.completed = false")
    List<MyLiveLecture> findAllUncompletedByLectureSchedule(
        @Param("lectureSchedule") LectureSchedule lectureSchedule);

    // 특정 스케줄의 수강생 수
    @Query("SELECT COUNT(ml) FROM MyLiveLecture ml " +
        "WHERE ml.lectureSchedule = :lectureSchedule")
    long countByLectureSchedule(@Param("lectureSchedule") LectureSchedule lectureSchedule);

    // 강의 수강생 모두 조회
    @Query("SELECT DISTINCT ml FROM MyLiveLecture ml " +
        "JOIN FETCH ml.lectureSchedule ls " +
        "JOIN FETCH ls.lecture l " +
        "WHERE l.liveId = :liveId")
    List<MyLiveLecture> findByLiveLectureLiveId(@Param("liveId") Long liveId);

    // completed = false인 수강생 조회
    @Query("SELECT ml FROM MyLiveLecture ml " +
        "JOIN ml.lectureSchedule ls " +
        "JOIN ls.lecture l " +
        "WHERE l.liveId = :liveId " +
        "AND ml.completed = false")
    List<MyLiveLecture> findActiveLecturesByLiveId(@Param("liveId") Long liveId);

    // 강의 id로 수강생 수 카운트
    @Query("SELECT COUNT(ml) FROM MyLiveLecture ml " +
        "JOIN ml.lectureSchedule ls " +
        "JOIN ls.lecture l " +
        "WHERE l.liveId = :liveId")
    long countByLiveLectureId(@Param("liveId") Long liveId);
}
