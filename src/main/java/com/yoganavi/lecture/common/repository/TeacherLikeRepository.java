package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.TeacherLike;
import com.yoganavi.lecture.common.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강사 좋아요 리포지토리 인터페이스
 */
public interface TeacherLikeRepository extends JpaRepository<TeacherLike, Long> {

    TeacherLike findByTeacherAndUser(Users teacher, Users user);
}
