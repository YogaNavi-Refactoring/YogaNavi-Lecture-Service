package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureLike;
import com.yoganavi.lecture.common.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordedLectureLikeRepository extends JpaRepository<RecordedLectureLike, Long> {

    @Query("SELECT COUNT(rl) FROM RecordedLectureLike rl WHERE rl.lecture.id = :lectureId")
    Long countLikesByLectureId(@Param("lectureId") Long lectureId);

    boolean existsByLectureAndUser(RecordedLecture lecture, Users user);

    void deleteByLectureAndUser(RecordedLecture lecture, Users user);
}