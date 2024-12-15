package com.yoganavi.live_lecture.controller;

import com.yoganavi.live_lecture.dto.HomeResponseDto;
import com.yoganavi.live_lecture.dto.SetIsOnAirDto;
import com.yoganavi.live_lecture.service.home.HomeService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/live-lecture/home")
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 페이지 요청 처리
     *
     * @return 홈 페이지에 대한 응답
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHomeData(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<HomeResponseDto> homeData = homeService.getHomeData(userId, page, size);

            response.put("message", "내 화상 강의 할 일 조회 성공");
            response.put("data", homeData);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {

            response.put("message", "내 화상강의 할 일 조회 실패");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 시그널링 서버 상태 업데이트
     *
     * @param setIsOnAirDto 시그널링 서버 상태 담은 dto
     * @return 업데이트 성공 여부
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateLiveStatus(
        @RequestBody SetIsOnAirDto setIsOnAirDto) {
        log.info("라이브 상태 업데이트 요청: 강의 ID {}, 상태 {}", setIsOnAirDto.getLiveId(),
            setIsOnAirDto.getOnAir());
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = homeService.updateLiveState(setIsOnAirDto.getLiveId(),
                setIsOnAirDto.getOnAir());
            if (result) {
                log.info("라이브 상태 업데이트 성공: 강의 ID {}", setIsOnAirDto.getLiveId());
                response.put("message", "success");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                log.warn("라이브 상태 업데이트 실패: 강의 ID {}", setIsOnAirDto.getLiveId());
                response.put("message", "fail");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(response);
            }
        } catch (NullPointerException e) {
            log.error("라이브 상태 업데이트 실패 (강의를 찾지 못함): 강의 ID {}", setIsOnAirDto.getLiveId());
            response.put("message", "fail");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("라이브 상태 업데이트 중 오류 발생: 강의 ID {}, 오류 메시지: {}", setIsOnAirDto.getLiveId(),
                e.getMessage());
            response.put("message", "fail");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
