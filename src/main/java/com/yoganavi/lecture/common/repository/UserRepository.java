package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {

}
