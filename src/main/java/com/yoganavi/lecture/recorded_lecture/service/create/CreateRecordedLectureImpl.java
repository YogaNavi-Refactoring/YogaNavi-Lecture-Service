package com.yoganavi.lecture.recorded_lecture.service.create;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureChapter;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.recorded_lecture.dto.ChapterDto;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
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
public class CreateRecordedLectureImpl implements CreateRecordedLecture {

    private final UserRepository userRepository;
    private final RecordedLectureRepository recordedLectureRepository;


    /**
     * 새로운 강의를 업로드
     *
     * @param lectureDto 사용자가 저장할 강의 정보
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void saveLecture(LectureDto lectureDto) {
        log.info("새 강의 저장 시작: 사용자 ID {}", lectureDto.getUserId());

        if (lectureDto == null) {
            throw new IllegalArgumentException("강의 정보가 비어있습니다.");
        }

        try {
            // 사용자 조회
            Users user = userRepository.findById(lectureDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id: " + lectureDto.getUserId()));

            // 기본 정보 검증
            if (lectureDto.getRecordTitle() == null || lectureDto.getRecordTitle().isEmpty()) {
                throw new IllegalArgumentException("강의 제목은 필수입니다.");
            }
            if (lectureDto.getRecordContent() == null || lectureDto.getRecordContent().isEmpty()) {
                throw new IllegalArgumentException("강의 내용은 필수입니다.");
            }
            if (lectureDto.getRecordedLectureChapters() == null
                || lectureDto.getRecordedLectureChapters().isEmpty()) {
                throw new IllegalArgumentException("최소 하나의 챕터가 필요합니다.");
            }

            RecordedLecture lecture = new RecordedLecture();
            lecture.setUser(user);
            lecture.setTitle(lectureDto.getRecordTitle());
            lecture.setContent(lectureDto.getRecordContent());
            lecture.setThumbnail(lectureDto.getRecordThumbnail());
            lecture.setThumbnailSmall(lectureDto.getRecordThumbnailSmall());

            // 챕터 정보 검증 및 저장
            int chapterNum = 1;
            List<RecordedLectureChapter> chapters = new ArrayList<>();
            for (ChapterDto chapterDto : lectureDto.getRecordedLectureChapters()) {
                try {
                    if (chapterDto.getChapterTitle() == null) {
                        throw new IllegalArgumentException(
                            "챕터" + chapterNum + " 의 제목이 비어있습니다.");
                    }

                    RecordedLectureChapter chapter = new RecordedLectureChapter();
                    chapter.setTitle(chapterDto.getChapterTitle());
                    chapter.setDescription(chapterDto.getChapterDescription());
                    chapter.setVideoUrl(chapterDto.getRecordVideo());
                    chapter.setLecture(lecture);
                    chapter.setChapterNumber(chapterNum);
                    chapters.add(chapter);
                    chapterNum++;
                } catch (Exception e) {
                    log.error("챕터 생성 중 오류 발생: 챕터 번호={}, error={}", chapterNum, e.getMessage());
                    throw new ServiceException("챕터 정보 처리 중 오류 발생", e);
                }
            }

            lecture.setChapters(chapters);

            try {
                recordedLectureRepository.save(lecture);
                log.info("새 강의 저장 완료: 강의 ID {}", lecture.getId());
            } catch (DataAccessException e) {
                log.error("강의 저장 중 데이터베이스 오류: userId={}, error={}",
                    lectureDto.getUserId(), e.getMessage());
                throw new ServiceException("강의 저장 중 데이터베이스 오류 발생", e);
            }

        } catch (EntityNotFoundException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 강의 정보: userId={}, error={}", lectureDto.getUserId(), e.getMessage());
            throw e;
        } catch (ServiceException e) {
            log.error("서비스 처리 중 오류: userId={}, error={}", lectureDto.getUserId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("강의 저장 중 예기치 않은 오류: userId={}, error={}",
                lectureDto.getUserId(), e.getMessage(), e);
            throw new ServiceException("강의를 저장하는 중 오류 발생했", e);
        }
    }
}
