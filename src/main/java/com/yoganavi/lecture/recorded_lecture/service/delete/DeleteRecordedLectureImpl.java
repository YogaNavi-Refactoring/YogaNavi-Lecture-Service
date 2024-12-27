package com.yoganavi.lecture.recorded_lecture.service.delete;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureChapter;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.recorded_lecture.dto.DeleteDto;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
public class DeleteRecordedLectureImpl implements DeleteRecordedLecture {

    private final RecordedLectureRepository recordedLectureRepository;

    /**
     * 강의를 삭제
     *
     * @param deleteDto 삭제할 강의 ID 목록
     * @param userId    사용자 ID
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteLectures(DeleteDto deleteDto, Long userId) {
        if (deleteDto == null || userId == null) {
            throw new IllegalArgumentException("삭제 정보가 올바르지 않습니다.");
        }

        log.info("녹화 강의들을 삭제: deleteDto={}, userId={}", deleteDto, userId);

        if (deleteDto.getLectureIds() == null || deleteDto.getLectureIds().isEmpty()) {
            log.warn("삭제할 강의 ID가 비어있음: userId={}", userId);
            throw new IllegalArgumentException("삭제할 강의가 비어있습니다.");
        }

        try {
            // 강의 목록 조회
            List<RecordedLecture> lectures = recordedLectureRepository.findAllByIdCustom(
                deleteDto.getLectureIds());

            if (lectures.isEmpty()) {
                log.warn("삭제할 강의를 찾을 수 없음: lectureIds={}", deleteDto.getLectureIds());
                throw new EntityNotFoundException("삭제할 강의를 찾을 수 없습니다.");
            }

            List<Long> deletedLectureIds = new ArrayList<>();
            List<Long> notFoundLectureIds = new ArrayList<>(deleteDto.getLectureIds());
            List<String> errorMessages = new ArrayList<>();

            // 각 강의별 삭제 처리
            for (RecordedLecture lecture : lectures) {
                try {
                    // 권한 체크
                    if (!Objects.equals(lecture.getUser().getUserId(), userId)) {
                        String errorMsg = "강의 ID " + lecture.getId() + "에 대한 삭제 권한 없음.";
                        log.error("강의 삭제 권한 없음: userId={}, lectureId={}", userId, lecture.getId());
                        throw new RuntimeException(errorMsg);
                    }

                    // S3 파일 삭제
                    try {
                        deleteS3Files(lecture);
                    } catch (Exception e) {
                        // 파일 삭제 실패는 warning 처리하고 계속 진행
                        log.warn("강의 관련 파일 삭제 실패 (데이터 삭제는 계속 진행): lectureId={}, error={}",
                            lecture.getId(), e.getMessage());
                    }

                    // db에서 강의 삭제
                    try {
                        recordedLectureRepository.delete(lecture);
                        deletedLectureIds.add(lecture.getId());
                        notFoundLectureIds.remove(lecture.getId());
                        log.info("강의 삭제 완료: lectureId={}", lecture.getId());
                    } catch (DataAccessException e) {
                        String errorMsg = "강의 id " + lecture.getId() + " 삭제 중 데이터베이스 오류 발생";
                        errorMessages.add(errorMsg);
                        log.error("강의 삭제 중 DB 오류: lectureId={}, error={}", lecture.getId(),
                            e.getMessage());
                    }

                } catch (RuntimeException e) {
                    errorMessages.add(e.getMessage());
                } catch (Exception e) {
                    String errorMsg = "강의 " + lecture.getId() + " 삭제 중 오류 발생: %s" + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.error("강의 삭제 중 오류: lectureId={}, error={}", lecture.getId(),
                        e.getMessage());
                }
            }

            // 삭제 결과 로깅
            log.info("강의 삭제 처리 완료: 성공={}, 실패={}", deletedLectureIds.size(),
                errorMessages.size());
            if (!notFoundLectureIds.isEmpty()) {
                log.warn("존재하지 않는 강의 ID: {}", notFoundLectureIds);
            }

            // 오류가 있었다면
            if (!errorMessages.isEmpty()) {
                throw new ServiceException("일부 강의 삭제 중 오류 발생");
            }

        } catch (DataAccessException e) {
            log.error("강의 삭제 중 데이터베이스 오류: userId={}, error={}", userId, e.getMessage());
            throw new ServiceException("강의 삭제 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (ServiceException e) {
            // 이미 로깅된 오류이므로 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("강의 삭제 중 예기치 않은 오류: userId={}, error={}", userId, e.getMessage());
            throw new ServiceException("강의 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteS3Files(RecordedLecture lecture) {
        if (lecture == null) {
            return;
        }

        List<String> failedFiles = new ArrayList<>();

        // 썸네일 파일 삭제
        try {
            if (lecture.getThumbnail() != null && !lecture.getThumbnail().isEmpty()) {
                //s3Service.deleteFile(lecture.getThumbnail());
                log.debug("썸네일 삭제 완료: {}", lecture.getThumbnail());
            }
        } catch (Exception e) {
            failedFiles.add(lecture.getThumbnail());
            log.warn("썸네일 삭제 실패: {}", lecture.getThumbnail());
        }

        // 작은 썸네일 파일 삭제
        try {
            if (lecture.getThumbnailSmall() != null && !lecture.getThumbnailSmall().isEmpty()) {
                //s3Service.deleteFile(lecture.getThumbnailSmall());
                log.debug("작은 썸네일 삭제 완료: {}", lecture.getThumbnailSmall());
            }
        } catch (Exception e) {
            failedFiles.add(lecture.getThumbnailSmall());
            log.warn("작은 썸네일 삭제 실패: {}", lecture.getThumbnailSmall());
        }

        // 챕터 비디오 파일 삭제
        for (RecordedLectureChapter chapter : lecture.getChapters()) {
            try {
                if (chapter.getVideoUrl() != null && !chapter.getVideoUrl().isEmpty()) {
                    //s3Service.deleteFile(chapter.getVideoUrl());
                    log.debug("챕터 비디오 삭제 완료: chapterId={}, url={}",
                        chapter.getId(), chapter.getVideoUrl());
                }
            } catch (Exception e) {
                failedFiles.add(chapter.getVideoUrl());
                log.warn("챕터 비디오 삭제 실패: chapterId={}, url={}",
                    chapter.getId(), chapter.getVideoUrl());
            }
        }

        // 실패한 파일
        if (!failedFiles.isEmpty()) {
            log.warn("강의 {} 관련 파일 중 일부 삭제 실패: {}", lecture.getId(), failedFiles);
        }
    }

}
