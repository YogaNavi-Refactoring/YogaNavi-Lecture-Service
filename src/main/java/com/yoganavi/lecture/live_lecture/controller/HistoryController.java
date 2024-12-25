package com.yoganavi.lecture.live_lecture.controller;

import com.yoganavi.lecture.live_lecture.dto.LectureHistoryDto;
import com.yoganavi.lecture.live_lecture.service.history.HistoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/live-lecture/mypage/course-history")
public class HistoryController {

    private final HistoryService hsitoryService;

    /**
     * 수강 내역 처리
     *
     * @return 수강 내역 페이지에 대한 응답 포함한 ResponseEntity
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHistoryData(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size) {

        log.info("수강 내역 조회 요청: 사용자 ID {}", userId);
        Map<String, Object> response = new HashMap<>();
        try {
            List<LectureHistoryDto> history = hsitoryService.getHistory(userId, page, size);
            log.info("수강 내역 조회 성공: 사용자 ID {}, 조회된 강의 수 {}", userId, history.size());
            response.put("message", "내 수강내역 조회 성공");
            response.put("data", history);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("수강 내역 조회 실패: 사용자 ID {}, 오류 : {}", userId, e.getMessage());
            response.put("message", "내 수강내역 조회 실패");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
