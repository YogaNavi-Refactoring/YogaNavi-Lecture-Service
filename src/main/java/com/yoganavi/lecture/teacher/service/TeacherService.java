package com.yoganavi.lecture.teacher.service;

import com.yoganavi.lecture.teacher.dto.DetailedTeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherFilter;
import java.util.List;

public interface TeacherService {


    List<TeacherDto> getAllTeachers(TeacherFilter filter, int sorting, long userId);

    List<TeacherDto> getSortedTeachers(int sorting, long userId, String keyword);

    DetailedTeacherDto getTeacherById(long teacherId, long userId);

    boolean toggleLike(long teacherId, long userId);

    List<TeacherDto> getLikeTeachers(long userId);

}
