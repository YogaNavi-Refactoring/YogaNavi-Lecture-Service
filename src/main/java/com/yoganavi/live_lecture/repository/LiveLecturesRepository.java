package com.yoganavi.live_lecture.repository;

import com.yoganavi.live_lecture.common.entity.LiveLectures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveLecturesRepository extends JpaRepository<LiveLectures, Long> {


}
