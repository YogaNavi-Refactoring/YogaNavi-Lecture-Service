package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordedLectureRepository extends JpaRepository<RecordedLecture, Long> {

    @Query("SELECT r FROM RecordedLecture r WHERE r.id IN :ids")
    List<RecordedLecture> findAllByIdCustom(@Param("ids") List<Long> ids);
}