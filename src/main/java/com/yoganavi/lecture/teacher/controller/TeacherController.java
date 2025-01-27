package com.yoganavi.lecture.teacher.controller;

import com.yoganavi.lecture.teacher.dto.DetailedTeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherFilter;
import com.yoganavi.lecture.teacher.service.TeacherService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/lecture/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTeachers(
        @RequestParam(defaultValue = "0") int sorting,
        @RequestParam(defaultValue = "0") long startTime,
        @RequestParam(defaultValue = "86340000") long endTime,
        @RequestParam(defaultValue = "MON,TUE,WED,THU,FRI,SAT,SUN,") String day,
        @RequestParam(defaultValue = "3") int period,
        @RequestParam(defaultValue = "2") int maxLiveNum,
        @RequestParam(defaultValue = "") String searchKeyword,
        @RequestHeader("X-User-Id") long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            validateParameters(sorting, period, maxLiveNum);

            TeacherFilter filter = createTeacherFilter(startTime, endTime, day, period, maxLiveNum,
                searchKeyword);
            List<TeacherDto> teachers = teacherService.getAllTeachers(filter, sorting, userId);

            response.put("message", "success");
            response.put("data", teachers);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 request parameters: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("선생 정보 조회 중 오류 발생", e);
            response.put("message", "서버 내부 오류가 발생했습니다");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/sort/{sorting}")
    public ResponseEntity<Map<String, Object>> getSortedTeachers(
        @PathVariable int sorting,
        @RequestParam(defaultValue = "") String searchKeyword,
        @RequestHeader("X-User-Id") long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            validateParameters(sorting);

            List<TeacherDto> teachers = teacherService.getSortedTeachers(sorting, userId,
                searchKeyword);
            response.put("message", "success");
            response.put("data", teachers);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 request parameters: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("선생 정보 정렬 중 에러 발생", e);
            response.put("message", "서버 내부 오류가 발생했습니다");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Map<String, Object>> getTeacherDetail(@PathVariable long teacherId,
        @RequestHeader("X-User-Id") long userId) { // 인증 토큰
        Map<String, Object> response = new HashMap<>();
        try {
            DetailedTeacherDto teacher = teacherService.getTeacherById(teacherId, userId);
            // 응답 생성
            response.put("message", "success");
            response.put("data", teacher);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 request parameters: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            // 오류 응답 생성
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "서버 내부 오류가 발생했습니다");
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/like/{teacherId}")
    public ResponseEntity<Map<String, Object>> likeTeacher(
        @PathVariable long teacherId,
        @RequestHeader("X-User-Id") long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isLiked = teacherService.toggleLike(teacherId, userId);
            response.put("message", isLiked ? "좋아요 성공" : "좋아요 취소");
            response.put("data", true);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 request parameters: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("선생 좋아요 중 에러 발생", e);
            response.put("message", "오류가 발생했습니다: " + e.getMessage());
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/my-like-teacher")
    public ResponseEntity<Map<String, Object>> getLikeTeachers(
        @RequestHeader("X-User-Id") long userId) {

        Map<String, Object> response = new HashMap<>();

        try {

            List<TeacherDto> likeTeachers = teacherService.getLikeTeachers(userId);
            response.put("message", "success");
            response.put("data", likeTeachers);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 request parameters: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("좋아요 한 선생 조회 중 에러 발생", e);
            response.put("message", "오류가 발생했습니다: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void validateParameters(int sorting, int period, int maxLiveNum) {
        if (sorting < 0 || sorting > 1) {
            throw new IllegalArgumentException("정렬 기준이 잘못되었습니다.");
        }
        if (period < 0 || period > 3) {
            throw new IllegalArgumentException("필터 기간이 잘못되었습니다.");
        }
        if (maxLiveNum < 0 || maxLiveNum > 2) {
            throw new IllegalArgumentException("최대 수강자 수가 잘못되었습니다.");
        }
    }

    private void validateParameters(int sorting) {

        if (sorting < 0 || sorting > 1) {
            throw new IllegalArgumentException("정렬 기준이 잘못되었습니다.");
        }
    }

    private TeacherFilter createTeacherFilter(long startTime, long endTime, String day,
        int period, int maxLiveNum, String searchKeyword) {
        TeacherFilter filter = new TeacherFilter();
        filter.setStartTime(startTime);
        filter.setEndTime(endTime);
        filter.setDay(day);
        filter.setPeriod(period);
        filter.setMaxLiveNum(maxLiveNum);
        filter.setSearchKeyword(searchKeyword);
        return filter;
    }
}