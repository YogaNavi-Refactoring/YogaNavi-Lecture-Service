package com.yoganavi.live_lecture.repository;

import com.yoganavi.live_lecture.common.entity.MyLiveLecture;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MyLiveLectureRepository extends JpaRepository<MyLiveLecture, Long> {

}
