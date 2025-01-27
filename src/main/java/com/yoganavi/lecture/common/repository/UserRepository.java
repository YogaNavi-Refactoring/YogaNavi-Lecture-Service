package com.yoganavi.lecture.common.repository;

import com.yoganavi.lecture.common.entity.Users;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<Users, Long> {

    @Query("SELECT tl.teacher FROM TeacherLike tl WHERE tl.user.userId = :userId")
    List<Users> findLikedTeachersByUserId(@Param("userId") long userId);
}
