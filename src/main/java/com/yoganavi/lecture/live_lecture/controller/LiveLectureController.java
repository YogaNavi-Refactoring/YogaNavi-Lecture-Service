package com.yoganavi.lecture.live_lecture.controller;

import com.yoganavi.lecture.live_lecture.dto.LiveLectureInfoDto;
import com.yoganavi.lecture.live_lecture.dto.LiveLectureResponseDto;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.live_lecture.service.lecture.LiveLectureService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실시간 강의 컨트롤러.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/live-lecture/manage")
public class LiveLectureController {

    private final LiveLectureService liveLectureService;
    private final UserRepository usersRepository;


    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLiveLecture(
        @RequestBody LiveLectureInfoDto liveLectureInfoDto,
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            liveLectureInfoDto.setUserId(userId);
            log.info("라이브 강의 생성 요청: 사용자 ID {}", userId);

            String userRole = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."))
                .getRole();

            if (!"TEACHER".equals(userRole)) {
                response.put("message", "권한이 없습니다");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            liveLectureService.createLiveLecture(liveLectureInfoDto);
            log.info("라이브 강의 생성 완료: 사용자 ID {}", userId);

            response.put("message", "화상강의 생성 성공");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("입력값 오류: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("라이브 강의 생성 중 오류 발생: {}", e.getMessage());
            response.put("message", "화상강의 생성 실패: " + e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLiveLecturesByUser(
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();
        log.info("라이브 강의 목록 조회 요청: 사용자 ID {}", userId);

        try {
            List<LiveLectureResponseDto> lectureList = liveLectureService.getLiveLecturesByUserId(
                userId);

            response.put("message", "화상 강의 조회 성공");
            response.put("data", lectureList);
            log.info("라이브 강의 목록 조회 완료: 사용자 ID {}, 조회된 강의 수 {}", userId, lectureList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "화상강의 조회 실패");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PutMapping("/update/{live_id}")
    public ResponseEntity<Map<String, Object>> updateLiveLecture(
        @PathVariable("live_id") Long liveId,
        @RequestBody LiveLectureInfoDto liveLectureInfoDto,
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("라이브 강의 수정 요청: 강의 ID {}, 사용자 ID {}", liveId, userId);
            liveLectureInfoDto.setLiveId(liveId);

            if (!liveLectureService.isLectureOwner(liveId, userId)) {
                log.warn("수정 권한이 없습니다. 강의 ID {}, 사용자 ID {}", liveId, userId);
                response.put("message", "수정 권한이 없습니다");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            liveLectureService.updateLiveLecture(liveLectureInfoDto);
            log.info("라이브 강의 수정 완료: 강의 ID {}", liveId);

            response.put("message", "화상강의 수정 성공");
            response.put("data", new Object[]{});
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("입력값 오류: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            log.error("강의 상태 오류: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("라이브 강의 수정 중 오류 발생: {}", e.getMessage());
            response.put("message", "강의 수정 중 오류가 발생했습니다: " + e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{live_id}")
    public ResponseEntity<Map<String, Object>> getLiveLectureById(
        @PathVariable("live_id") Long liveId,
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();
        log.info("단일 라이브 강의 조회 요청: 강의 ID {}, 사용자 ID {}", liveId, userId);

        try {
            LiveLectureResponseDto lecture = liveLectureService.getLiveLectureById(liveId);

            if (lecture == null) {
                response.put("message", "강의 없음");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (!liveLectureService.isLectureOwner(liveId, userId)) {
                response.put("message", "권한이 없습니다");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            log.info("단일 라이브 강의 조회 완료: 강의 ID {}", liveId);
            response.put("message", "조회에 성공했습니다");
            response.put("data", lecture);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("단일 라이브 강의 조회 중 오류 발생: 강의 ID {}, 오류 메시지 {}", liveId, e.getMessage());
            response.put("message", "강의 조회 중 오류가 발생했습니다: ");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{live_id}")
    public ResponseEntity<Map<String, Object>> deleteLiveLecture(
        @PathVariable("live_id") Long liveId,
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("라이브 강의 삭제 요청: 강의 ID {}, 사용자 ID {}", liveId, userId);

            LiveLectureResponseDto lecture = liveLectureService.getLiveLectureById(liveId);
            if (lecture == null) {
                response.put("message", "강의가 존재하지 않습니다");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (!liveLectureService.isLectureOwner(liveId, userId)) {
                response.put("message", "삭제 권한이 없습니다");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            liveLectureService.deleteLiveLectureById(liveId);
            log.info("라이브 강의 삭제 완료: 강의 ID {}", liveId);

            response.put("message", "강의 삭제 성공");
            response.put("data", new Object[]{});
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("강의 상태 오류: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (IllegalArgumentException e) {
            log.error("입력값 오류: {}", e.getMessage());
            response.put("message", e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("라이브 강의 삭제 중 오류 발생: {}", e.getMessage());
            response.put("message", "강의 삭제 중 오류가 발생했습니다: " + e.getMessage());
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}