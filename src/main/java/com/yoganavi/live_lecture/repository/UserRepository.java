package com.yoganavi.live_lecture.repository;

import com.yoganavi.live_lecture.common.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {

}
