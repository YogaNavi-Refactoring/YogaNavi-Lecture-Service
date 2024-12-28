package com.yoganavi.lecture.recorded_lecture.service.show;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.AllRecordedLecturesRepository;
import com.yoganavi.lecture.common.repository.MyLikeLectureListRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureListRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import com.yoganavi.lecture.recorded_lecture.service.recordedLectureServiceUtil.RecordedLectureServiceUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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
public class ShowRecordedLectureImpl implements ShowRecordedLecture {

    private final RecordedLectureServiceUtil recordedLectureServiceUtil;
    private final UserRepository userRepository;
    private final MyLikeLectureListRepository myLikeLectureListRepository;
    private final RecordedLectureListRepository recordedLectureListRepository;
    private final RecordedLectureRepository recordedLectureRepository;
    private final AllRecordedLecturesRepository allRecordedLecturesRepository;

    /**
     * 사용자가 업로드한 강의 목록을 조회
     *
     * @param userId 사용자 ID
     * @return 사용자가 업로드한 강의 목록 (LectureDto 리스트)
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LectureDto> getMyLectures(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id: " + userId));

            return recordedLectureListRepository.findAllLectures(user);

        } catch (EntityNotFoundException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("데이터베이스 접근 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            throw new ServiceException("강의 목록 조회 중 데이터베이스 오류가 발생", e);
        } catch (Exception e) {
            log.error("강의 목록 조회 중 예기치 않은 오류: userId={}, error={}", userId, e.getMessage(), e);
            throw new ServiceException("강의 목록을 조회하는 중 오류가 발생", e);
        }
    }

    /**
     * 사용자가 좋아요한 강의 목록을 조회
     *
     * @param userId 사용자 ID
     * @return 사용자가 좋아요한 강의 목록 (LectureDto 리스트)
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<LectureDto> getLikeLectures(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id: " + userId));

            return myLikeLectureListRepository.findMyLikedLectures(user);

        } catch (EntityNotFoundException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("좋아요 강의 목록 조회 중 데이터베이스 오류: userId={}, error={}", userId, e.getMessage());
            throw new ServiceException("강의 목록 조회 중 데이터베이스 오류가 발생", e);
        } catch (Exception e) {
            log.error("좋아요 강의 목록 조회 중 예기치 않은 오류: userId={}, error={}", userId, e.getMessage(), e);
            throw new ServiceException("좋아요 목록을 조회하는 중 오류가 발생", e);
        }
    }

    /**
     * 강의 내용 상세 정보를 조회
     *
     * @param recordedId 강의 ID
     * @param userId     사용자 ID
     * @return 강의 상세 정보
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public LectureDto getLectureDetails(long recordedId, Long userId) {
        log.info("강의 상세 정보 조회 시작: 강의 id {}, 사용자 id {}", recordedId, userId);

        try {
            // 강의 정보 조회
            RecordedLecture lecture = recordedLectureRepository.findById(recordedId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "강의를 찾을 수 없습니다. id: " + recordedId));

            // 사용자 정보 조회
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id: " + userId));

            LectureDto dto = recordedLectureServiceUtil.convertToDto(lecture);

            try {
                // 좋아요 정보 조회 - 실패해도 기본 강의 정보 반환
                recordedLectureServiceUtil.getLikeInformation(dto, lecture, user);
            } catch (Exception e) {
                log.warn("좋아요 정보 조회 실패: lectureId={}, userId={}, error={}",
                    recordedId, userId, e.getMessage());
                // 좋아요 정보 조회 실패 시 기본값 0
                dto.setLikeCount(0L);
                dto.setMyLike(false);
            }

            log.info("강의 상세 정보 조회 완료: 강의 id {}", recordedId);
            return dto;

        } catch (EntityNotFoundException e) {
            log.error("엔티티 조회 실패: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("강의 상세 정보 조회 중 데이터베이스 오류: lectureId={}, userId={}, error={}",
                recordedId, userId, e.getMessage());
            throw new ServiceException("강의 정보 조회 중 데이터베이스 오류 발생", e);
        } catch (Exception e) {
            log.error("강의 상세 정보 조회 중 예기치 않은 오류: lectureId={}, userId={}, error={}",
                recordedId, userId, e.getMessage(), e);
            throw new ServiceException("강의 정보를 조회하는 중 오류 발생", e);
        }
    }

    @Override
    public List<LectureDto> getAllLectures(Long userId, int page, int size, String sort) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        try {
            // 사용자 조회
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id" + userId));

            try {
                // 강의 목록 조회
                List<LectureDto> lectureDtos = allRecordedLecturesRepository.findAllLectures(
                    user, page, size, sort);

                log.info("전체 강의 목록 조회 완료: userId={}, page={}, size={}, resultSize={}",
                    userId, page, size, lectureDtos.size());

                return lectureDtos;
            } catch (DataAccessException e) {
                log.error("강의 목록 조회 중 데이터베이스 오류: userId={}, page={}, size={}, error={}",
                    userId, page, size, e.getMessage());
                throw new ServiceException("강의 목록 조회 중 데이터베이스 오류 발생", e);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("강의 목록 조회 중 예기치 않은 오류: userId={}, page={}, size={}, error={}",
                userId, page, size, e.getMessage());
            throw new ServiceException("강의 목록을 조회하는 중 오류 발생", e);
        }
    }
}
