package com.yoganavi.lecture.recorded_lecture.service.update;
import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureChapter;
import com.yoganavi.lecture.common.repository.AllRecordedLecturesRepository;
import com.yoganavi.lecture.common.repository.MyLikeLectureListRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureLikeRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureListRepository;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.recorded_lecture.dto.ChapterDto;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateRecordedLectureImpl implements UpdateRecordedLecture{
    private final UserRepository userRepository;
    private final MyLikeLectureListRepository myLikeLectureListRepository;
    private final RecordedLectureListRepository recordedLectureListRepository;
    private final RecordedLectureLikeRepository recordedLectureLikeRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecordedLectureRepository recordedLectureRepository;
    private final RecordedLectureLikeRepository lectureLikeRepository;
    private final AllRecordedLecturesRepository allRecordedLecturesRepository;

    /**
     * 강의 정보를 업데이트
     *
     * @param lectureDto 업데이트할 강의 정보
     * @return 업데이트 성공 여부
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public boolean updateLecture(LectureDto lectureDto) {
        if (lectureDto == null || lectureDto.getRecordedId() == null) {
            log.error("강의정보가 입력되지 않았습니다.");
            throw new IllegalArgumentException("강의정보가 입력되지 않았습니다.");
        }

        log.info("강의 수정 시작: 강의 ID {}", lectureDto.getRecordedId());
        try {
            RecordedLecture lecture = recordedLectureRepository.findById(lectureDto.getRecordedId())
                .orElseThrow(
                    () -> new RuntimeException("강의를 찾을 수 없습니다. id: " + lectureDto.getRecordedId()));

            if (!Objects.equals(lecture.getUser().getUserId(), lectureDto.getUserId())) {
                throw new RuntimeException("강의를 수정할 권한이 없습니다. id: " + lectureDto.getUserId());
            }
            try {
                validateUpdateLectureDto(lectureDto);

                // 강의 기본 정보 업데이트 (제목, 내용, 썸네일)
                updateLectureDetails(lecture, lectureDto);

                // 강의 챕터 정보 업데이트 (추가, 수정, 삭제)
                updateChapters(lecture, lectureDto.getRecordedLectureChapters());

                // 변경된 강의 정보를 데이터베이스에 저장
                recordedLectureRepository.save(lecture);

                log.info("강의 수정 성공: 강의 ID {}", lectureDto.getRecordedId());
                return true;

            } catch (DataAccessException e) {
                log.error("강의 수정 중 데이터베이스 오류: lectureId={}, error={}",
                    lectureDto.getRecordedId(), e.getMessage());
                throw new ServiceException("강의 수정 중 데이터베이스 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            log.error("강의 수정 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    private void validateUpdateLectureDto(LectureDto lectureDto) {
        if (lectureDto.getRecordTitle() == null || lectureDto.getRecordTitle().isEmpty()) {
            throw new IllegalArgumentException("강의 제목은 필수입니다.");
        }
        if (lectureDto.getRecordContent() == null || lectureDto.getRecordContent().isEmpty()) {
            throw new IllegalArgumentException("강의 내용은 필수입니다.");
        }
        if (lectureDto.getRecordedLectureChapters() == null ||
            lectureDto.getRecordedLectureChapters().isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 챕터가 필요합니다.");
        }
    }

    /**
     * 강의의 기본 정보를 업데이트
     *
     * @param lecture    기존 강의 엔티티
     * @param lectureDto 새로운 강의 정보가 담긴 DTO
     */
    private void updateLectureDetails(RecordedLecture lecture, LectureDto lectureDto) {
        try {
            // 강의 제목 업데이트
            if (!Objects.equals(lecture.getTitle(), lectureDto.getRecordTitle())) {
                lecture.setTitle(lectureDto.getRecordTitle());
                log.info("강의 제목 업데이트: {}", lectureDto.getRecordTitle());
            }

            // 강의 내용 업데이트
            if (!Objects.equals(lecture.getContent(), lectureDto.getRecordContent())) {
                lecture.setContent(lectureDto.getRecordContent());
                log.info("강의 내용 업데이트");
            }

            // 썸네일 업데이트
            updateThumbnail(lecture, lectureDto);

        } catch (Exception e) {
            log.error("강의 기본 정보 업데이트 중 오류: lectureId={}, error={}",
                lecture.getId(), e.getMessage());
            throw new ServiceException("강의 정보 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    private void updateThumbnail(RecordedLecture lecture, LectureDto lectureDto) {
        try {
            // 메인 썸네일 업데이트
            if (!Objects.equals(lecture.getThumbnail(), lectureDto.getRecordThumbnail())) {
                // 기존 썸네일 삭제
                if (lecture.getThumbnail() != null) {
                    try {
                        //s3Service.deleteFile(lecture.getThumbnail());
                    } catch (Exception e) {
                        log.warn("기존 썸네일 삭제 실패: {}", lecture.getThumbnail());
                    }
                }
                lecture.setThumbnail(lectureDto.getRecordThumbnail());
                log.info("강의 썸네일 업데이트: {}", lectureDto.getRecordThumbnail());
            }

            // 작은 썸네일 업데이트
            if (!Objects.equals(lecture.getThumbnailSmall(),
                lectureDto.getRecordThumbnailSmall())) {
                if (lecture.getThumbnailSmall() != null) {
                    try {
                        //s3Service.deleteFile(lecture.getThumbnailSmall());
                    } catch (Exception e) {
                        log.warn("기존 작은 썸네일 삭제 실패: {}", lecture.getThumbnailSmall());
                    }
                }
                lecture.setThumbnailSmall(lectureDto.getRecordThumbnailSmall());
                log.info("강의 소형 썸네일 업데이트: {}", lectureDto.getRecordThumbnailSmall());
            }
        } catch (Exception e) {
            log.error("썸네일 업데이트 중 오류: lectureId={}, error={}", lecture.getId(), e.getMessage());
            throw new ServiceException("썸네일 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    private void updateChapters(RecordedLecture lecture, List<ChapterDto> chapterDtos) {
        if (lecture == null) {
            throw new IllegalArgumentException("강의 정보가 null일 수 없습니다.");
        }
        if (chapterDtos == null) {
            throw new IllegalArgumentException("챕터 정보 리스트가 null일 수 없습니다.");
        }

        try {
            // 현재 강의의 모든 챕터 id set으로
            Set<Long> existingChapterIds = new HashSet<>();
            for (RecordedLectureChapter chapter : lecture.getChapters()) {
                existingChapterIds.add(chapter.getId());
            }

            // 프론트에서 보낸 챕터 ID를 set으로 저장
            Set<Long> receivedChapterIds = new HashSet<>();
            for (ChapterDto chapterDto : chapterDtos) {
                if (chapterDto.getId() != 0) { // 새로 추가된 챕터는 ID가 0이므로 제외
                    receivedChapterIds.add(chapterDto.getId());
                }
            }

            // 삭제될 챕터 ID
            Set<Long> chapterIdsToDelete = new HashSet<>(existingChapterIds);
            chapterIdsToDelete.removeAll(receivedChapterIds);

            // 챕터 삭제 처리
            Iterator<RecordedLectureChapter> iterator = lecture.getChapters().iterator();
            while (iterator.hasNext()) {
                RecordedLectureChapter chapter = iterator.next();
                if (chapterIdsToDelete.contains(chapter.getId())) {
                    try {
                        if (chapter.getVideoUrl() != null && !chapter.getVideoUrl().isEmpty()) {
                            //s3Service.deleteFile(chapter.getVideoUrl());
                        }
                        iterator.remove();
                        log.info("챕터 삭제: {}", chapter.getId());
                    } catch (Exception e) {
                        log.warn("챕터 {} 삭제 중 파일 처리 오류: {}", chapter.getId(), e.getMessage());
                    }
                }
            }

            // 챕터 업데이트 및 새 챕터 추가
            for (ChapterDto chapterDto : chapterDtos) {
                if (chapterDto == null) {
                    log.warn("null인 챕터 정보가 포함되어 있습니다.");
                    continue;
                }

                try {
                    if (chapterDto.getChapterTitle() == null || chapterDto.getChapterTitle()
                        .isEmpty()) {
                        throw new IllegalArgumentException("챕터 제목은 필수입니다.");
                    }

                    if (chapterDto.getId() != 0) {
                        // 기존 챕터 업데이트
                        RecordedLectureChapter chapter = null;
                        for (RecordedLectureChapter c : lecture.getChapters()) {
                            if (c.getId() == chapterDto.getId()) {
                                chapter = c;
                                break;
                            }
                        }
                        if (chapter != null) {
                            updateChapter(chapter, chapterDto);
                        } else {
                            log.warn("업데이트할 챕터 찾을 수 없음: {}", chapterDto.getId());
                        }
                    } else {
                        // 새 챕터 추가
                        RecordedLectureChapter newChapter = createChapter(chapterDto, lecture);
                        lecture.getChapters().add(newChapter);
                        log.info("새 챕터 추가: {}", newChapter.getTitle());
                    }
                } catch (IllegalArgumentException e) {
                    log.error("챕터 데이터 유효성 검사 실패: {}", e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("챕터 처리 중 오류 발생: {}", e.getMessage());
                    throw new ServiceException("챕터 처리 중 오류 발생", e);
                }
            }
        } catch (Exception e) {
            log.error("챕터 업데이트 중 오류 발생: lectureId={}, error={}",
                lecture.getId(), e.getMessage());
            throw new ServiceException("챕터 정보 업데이트 중 오류 발생", e);
        }
    }

    private void updateChapter(RecordedLectureChapter chapter, ChapterDto chapterDto) {
        try {
            // 챕터 제목 업데이트 (변경된 경우에만)
            if (!Objects.equals(chapter.getTitle(), chapterDto.getChapterTitle())) {
                if (chapterDto.getChapterTitle() == null || chapterDto.getChapterTitle()
                    .isEmpty()) {
                    throw new IllegalArgumentException("챕터 제목은 필수입니다.");
                }
                chapter.setTitle(chapterDto.getChapterTitle());
                log.info("챕터 제목 업데이트: {}", chapter.getId());
            }

            // 챕터 설명 업데이트 (변경된 경우에만)
            if (!Objects.equals(chapter.getDescription(), chapterDto.getChapterDescription())) {
                chapter.setDescription(chapterDto.getChapterDescription());
                log.info("챕터 설명 업데이트: {}", chapter.getId());
            }

            // 챕터 번호 업데이트 (변경된 경우에만)
            if (chapter.getChapterNumber() != chapterDto.getChapterNumber()) {
                if (chapterDto.getChapterNumber() <= 0) {
                    throw new IllegalArgumentException("챕터 번호는 1 이상이어야 합니다.");
                }
                chapter.setChapterNumber(chapterDto.getChapterNumber());
                log.info("챕터 번호 업데이트: {}", chapter.getId());
            }

            // 비디오 URL 업데이트
            String existingUrl = chapter.getVideoUrl();
            String newUrl = chapterDto.getRecordVideo();

            if (!Objects.equals(existingUrl, newUrl)) {
                try {
                    if (newUrl != null && !newUrl.isEmpty()) {
                        // 새 비디오 URL이 제공된 경우
                        if (existingUrl != null && !existingUrl.isEmpty()) {
                            //s3Service.deleteFile(existingUrl);
                        }
                        chapter.setVideoUrl(newUrl);
                        log.info("챕터 비디오 URL 업데이트: {} -> {}", chapter.getId(), newUrl);
                    } else if (existingUrl != null && !existingUrl.isEmpty()) {
                        // 새 URL이 없고 기존 URL이 있는 경우 (비디오 삭제)
                        //s3Service.deleteFile(existingUrl);
                        chapter.setVideoUrl(null);
                        log.info("챕터 비디오 삭제: {}", chapter.getId());
                    }
                } catch (Exception e) {
                    log.warn("챕터 {} 비디오 파일 처리 중 오류: {}", chapter.getId(), e.getMessage());
                    // 파일 처리 실패는 warning으로 처리하고 계속 진행
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("챕터 업데이트 중 오류: chapterId={}, error={}", chapter.getId(), e.getMessage());
            throw new ServiceException("챕터 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    private RecordedLectureChapter createChapter(ChapterDto chapterDto, RecordedLecture lecture) {
        if (chapterDto.getChapterTitle() == null || chapterDto.getChapterTitle().isEmpty()) {
            throw new IllegalArgumentException("챕터 제목은 필수입니다.");
        }
        if (chapterDto.getChapterNumber() <= 0) {
            throw new IllegalArgumentException("챕터 번호는 1 이상이어야 함");
        }

        try {
            RecordedLectureChapter chapter = new RecordedLectureChapter();
            chapter.setTitle(chapterDto.getChapterTitle());
            chapter.setDescription(chapterDto.getChapterDescription());
            chapter.setChapterNumber(chapterDto.getChapterNumber());
            chapter.setVideoUrl(chapterDto.getRecordVideo());
            chapter.setLecture(lecture);
            return chapter;
        } catch (Exception e) {
            log.error("새 챕터 생성 중 오류 발생: {}", e.getMessage());
            throw new ServiceException("새 챕터 생성 중 오류 발생", e);
        }
    }
}
