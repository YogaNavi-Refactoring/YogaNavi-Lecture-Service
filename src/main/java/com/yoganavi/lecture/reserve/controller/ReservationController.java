package com.yoganavi.lecture.reserve.controller;

import com.yoganavi.lecture.reserve.dto.LiveLectureDto;
import com.yoganavi.lecture.reserve.dto.ReservationInfoDto;
import com.yoganavi.lecture.reserve.dto.ReservationRequestDto;
import com.yoganavi.lecture.reserve.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/lecture/teacher/reserve")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReservation(
        @RequestBody ReservationRequestDto reservationRequest,
        @RequestHeader("X-User-Id") long userId) {

        Map<String, Object> response = new HashMap<>();
        try {
            reservationService.createReservation(userId, reservationRequest);

            response.put("message", "success");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("수강신청 실패", e);
            response.put("message", "서버 내부 오류가 발생했습니다: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLiveLectures(@RequestParam int method) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<LiveLectureDto> lectures = reservationService.getAllLiveLectures(method);
            response.put("message", "success");
            response.put("data", lectures);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("라이브강의 조회 실패. 메서드", e);
            response.put("message", "서버 내부 오류가 발생했습니다");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{teacherId}")
    public ResponseEntity<Map<String, Object>> getLiveLecturesByTeacherAndMethod(
        @PathVariable long teacherId,
        @RequestParam int method) {

        Map<String, Object> response = new HashMap<>();
        try {
            List<ReservationInfoDto> lectures = reservationService.getLiveLecturesByTeacherAndMethod(
                teacherId, method);

            response.put("message", "success");
            response.put("data", lectures);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("라이브강의 조회 실패. 메서드+강사", e);
            response.put("message", "서버 내부 오류가 발생했습니다: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
