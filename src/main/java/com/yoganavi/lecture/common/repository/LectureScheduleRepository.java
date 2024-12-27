package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.LectureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LectureScheduleRepository extends JpaRepository<LectureSchedule, Long> {

}
