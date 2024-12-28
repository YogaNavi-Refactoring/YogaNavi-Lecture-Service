package com.yoganavi.lecture.recorded_lecture.controller;

import com.yoganavi.lecture.recorded_lecture.dto.DeleteDto;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import com.yoganavi.lecture.recorded_lecture.service.create.CreateRecordedLecture;
import com.yoganavi.lecture.recorded_lecture.service.delete.DeleteRecordedLecture;
import com.yoganavi.lecture.recorded_lecture.service.like.LikeRecordedLecture;
import com.yoganavi.lecture.recorded_lecture.service.search.SearchRecordedLecture;
import com.yoganavi.lecture.recorded_lecture.service.show.ShowRecordedLecture;
import com.yoganavi.lecture.recorded_lecture.service.update.UpdateRecordedLecture;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/recorded-lecture")
public class RecordController {

    private final CreateRecordedLecture createRecordedLecture;
    private final DeleteRecordedLecture deleteRecordedLecture;
    private final LikeRecordedLecture likeRecordedLecture;
    private final SearchRecordedLecture searchRecordedLecture;
    private final ShowRecordedLecture showRecordedLecture;
    private final UpdateRecordedLecture updateRecordedLecture;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getMyLectures(
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();
        log.info("사용자 ID: {}의 녹화 강의 목록 조회 요청", userId);
        try {
            List<LectureDto> lectureList = showRecordedLecture.getMyLectures(userId);

            if (!lectureList.isEmpty()) {
                log.info("사용자 ID: {}의 녹화 강의 {}개 조회 성공", userId, lectureList.size());
                response.put("message", "녹화강의 조회 성공");
                response.put("data", lectureList);
                return ResponseEntity.ok(response);
            } else {
                log.info("사용자 ID: {}의 녹화 강의 없음", userId);
                response.put("message", "녹화강의 없음");
                response.put("data", new Object() {
                });
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 사용자가 좋아요한 강의 목록 조회
     *
     * @return 사용자가 업로드한 강의 목록과 관련된 정보가 포함된 ResponseEntity
     */
    @GetMapping("/likelist")
    public ResponseEntity<Map<String, Object>> getLikeLectures(
        @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<LectureDto> lectureList = showRecordedLecture.getLikeLectures(userId);

            if (!lectureList.isEmpty()) {
                response.put("message", "좋아요한 녹화강의 조회 성공");
                response.put("data", lectureList);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "녹화강의 없음");
                response.put("data", new Object() {
                });
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "좋아요한 녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "좋아요한 녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "좋아요한 녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 새로운 강의 생성 요청
     *
     * @param lectureDto 강의 정보
     * @return sessionId
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLecture(
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody LectureDto lectureDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("사용자 ID: {}의 녹화 강의 생성 요청", userId);
            lectureDto.setUserId(userId);
            createRecordedLecture.saveLecture(lectureDto);
            log.info("사용자 ID: {}의 녹화 강의 생성 성공", userId);

            response.put("message", "강의가 성공적으로 생성되었습니다.");
            response.put("data", true);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "강의 생성 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값: {}", e.getMessage());
            response.put("message", "강의 생성 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "강의 생성 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "강의 생성 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 업로드한 강의 상세 정보 조회
     *
     * @param recorded_id 강의 id
     * @return 강의 상세 정보
     */
    @GetMapping("/recorded-lecture/detail/{recorded_id}")
    public ResponseEntity<Map<String, Object>> getLectureDetails(
        @RequestHeader("X-User-Id") Long userId, @PathVariable long recorded_id) {
        Map<String, Object> response = new HashMap<>();
        try {
            LectureDto lectureDto = showRecordedLecture.getLectureDetails(recorded_id, userId);
            response.put("message", "녹화강의 조회 성공");
            response.put("data", lectureDto);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("강의 또는 사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "녹화강의 조회 실패");
            response.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 강의의 정보 수정
     *
     * @param lectureDto 수정된 강의 dto
     * @return 강의 수정 성공/실패 응답
     */
    @PutMapping("/recorded-lecture/update/{recordedId}")
    public ResponseEntity<Map<String, Object>> updateLecture(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long recordedId,
        @RequestBody LectureDto lectureDto) {
        Map<String, Object> response = new HashMap<>();
        lectureDto.setRecordedId(recordedId);
        lectureDto.setUserId(userId);

        try {
            log.info("강의 수정 요청: 강의 ID {}, 사용자 ID {}", recordedId, userId);
            boolean updateResult = updateRecordedLecture.updateLecture(lectureDto);

            response.put("message", "강의 수정 성공");
            response.put("data", updateResult);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("강의를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "강의 수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값: {}", e.getMessage());
            response.put("message", "강의 수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "강의 수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (RuntimeException e) {
            log.error("권한 없음: {}", e.getMessage());
            response.put("message", "강의 수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "강의 수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 강의 삭제 요청
     *
     * @param deleteDto 삭제할 강의 ID 리스트를 포함한 DTO
     * @return 강의 삭제 성공/실패 응답
     */
    @PostMapping("/recorded-lecture/delete")
    public ResponseEntity<Map<String, Object>> deleteLectures(
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody DeleteDto deleteDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("사용자 {}의 강의 삭제 요청: {}", userId, deleteDto.getLectureIds());
            deleteRecordedLecture.deleteLectures(deleteDto, userId);

            response.put("message", "강의 삭제 성공");
            response.put("data", true);
            log.info("사용자 {}의 강의 삭제 성공", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값: {}", e.getMessage());
            response.put("message", "강의 삭제 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EntityNotFoundException e) {
            log.error("강의를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "강의 삭제 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "강의 삭제 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (RuntimeException e) {
            log.error("권한 없음: {}", e.getMessage());
            response.put("message", "강의 삭제 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "강의 삭제 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 강의 좋아요, 좋아요 취소
     *
     * @param recordedId 강의 id
     * @return 좋아요/취소 성공/실패 응답
     */
    @PostMapping("/recorded-lecture/like/{recordedId}")
    public ResponseEntity<Map<String, Object>> like(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long recordedId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isLiked = likeRecordedLecture.toggleLike(recordedId, userId);
            log.info("사용자 {}가 강의 {} 좋아요/취소", userId, recordedId);
            response.put("message", isLiked ? "좋아요 성공" : "좋아요 취소");
            response.put("data", isLiked);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("강의 또는 사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "수정 실패");
            response.put("data", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/recorded-lecture/sort/{sort}")
    public ResponseEntity<Map<String, Object>> getAllLectures(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "21") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("사용자 ID: {}", userId);
            List<LectureDto> lectureList = showRecordedLecture.getAllLectures(userId, page, size,
                sort);

            if (!lectureList.isEmpty()) {
                response.put("message", "녹화강의 조회 성공");
                response.put("data", lectureList);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "녹화강의 없음");
                response.put("data", new Object() {
                });
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: {}", e.getMessage());
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "녹화강의 조회 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/recorded-lecture/search/{keyword}/sort/{sort}")
    public ResponseEntity<Map<String, Object>> searchLectures(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable String keyword,
        @PathVariable String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "21") int size,
        @RequestParam(defaultValue = "false") boolean title,
        @RequestParam(defaultValue = "false") boolean content) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<LectureDto> lectureList = searchRecordedLecture.searchLectures(userId, keyword,
                sort,
                page,
                size, title, content);

            if (!lectureList.isEmpty()) {
                response.put("message", "강의 검색 성공");
                response.put("data", lectureList);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "검색 결과 없음");
                response.put("data", new Object() {
                });
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자를 찾을 수 없음: {}", e.getMessage());
            response.put("message", "강의 검색 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값: {}", e.getMessage());
            response.put("message", "강의 검색 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (ServiceException e) {
            log.error("검색 처리 중 오류: {}", e.getMessage());
            response.put("message", "강의 검색 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            log.error("예기치 않은 오류: {}", e.getMessage(), e);
            response.put("message", "강의 검색 실패");
            response.put("data", new Object() {
            });
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
