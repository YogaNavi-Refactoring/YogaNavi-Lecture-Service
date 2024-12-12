package com.yoganavi.live_lecture.repository;

import com.yoganavi.live_lecture.common.entity.LectureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LectureScheduleRepository extends JpaRepository<LectureSchedule, Integer> {

}
