package com.yoganavi.lecture.recorded_lecture.service.like;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureLike;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.RecordedLectureLikeRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class LikeRecordedLectureImpl implements LikeRecordedLecture {

    private final UserRepository userRepository;
    private final RecordedLectureRepository recordedLectureRepository;
    private final RecordedLectureLikeRepository lectureLikeRepository;

    /**
     * 강의에 대한 좋아요를 토글
     *
     * @param recordedId 강의 ID
     * @param userId     사용자 ID
     * @return 좋아요 상태 (true: 좋아요 추가, false: 좋아요 취소)
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean toggleLike(Long recordedId, Long userId) {
        if (recordedId == null || userId == null) {
            throw new IllegalArgumentException("강의 ID와 사용자 ID는 필수입니다.");
        }

        try {
            // 강의 조회
            RecordedLecture lecture = recordedLectureRepository.findById(recordedId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "강의를 찾을 수 없습니다. id: " + recordedId));

            // 사용자 조회
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id: " + userId));

            try {
                boolean exists = lectureLikeRepository.existsByLectureAndUser(lecture, user);

                if (exists) {
                    // 좋아요 취소
                    try {
                        lectureLikeRepository.deleteByLectureAndUser(lecture, user);
                        log.info("강의 좋아요 취소 완료: lectureId={}, userId={}", recordedId, userId);
                        return false;
                    } catch (DataAccessException e) {
                        log.error("좋아요 취소 중 데이터베이스 오류: lectureId={}, userId={}, error={}",
                            recordedId, userId, e.getMessage());
                        throw new ServiceException("좋아요 취소 중 오류 발생", e);
                    }
                } else {
                    // 좋아요 추가
                    try {
                        RecordedLectureLike like = new RecordedLectureLike();
                        like.setLecture(lecture);
                        like.setUser(user);
                        lectureLikeRepository.save(like);
                        log.info("강의 좋아요 추가 완료: lectureId={}, userId={}", recordedId, userId);
                        return true;
                    } catch (DataAccessException e) {
                        log.error("좋아요 추가 중 데이터베이스 오류: lectureId={}, userId={}, error={}",
                            recordedId, userId, e.getMessage());
                        throw new ServiceException("좋아요 추가 중 오류 발생", e);
                    }
                }
            } catch (DataAccessException e) {
                log.error("좋아요 상태 확인 중 데이터베이스 오류: lectureId={}, userId={}, error={}",
                    recordedId, userId, e.getMessage());
                throw new ServiceException("좋아요 상태 확인 중 오류 발생", e);
            }
        } catch (EntityNotFoundException e) {
            log.error("좋아요 처리 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("좋아요 처리 중 예기치 않은 오류: lectureId={}, userId={}, error={}",
                recordedId, userId, e.getMessage());
            throw new ServiceException("좋아요 처리 중 오류 발생", e);
        }
    }
}
