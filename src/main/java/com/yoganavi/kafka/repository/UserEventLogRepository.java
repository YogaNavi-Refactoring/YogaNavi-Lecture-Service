package com.yoganavi.kafka.repository;

import com.yoganavi.kafka.entity.UserEventLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEventLogRepository extends JpaRepository<UserEventLog, Long> {

    List<UserEventLog> findByUserId(Long userId);
}
