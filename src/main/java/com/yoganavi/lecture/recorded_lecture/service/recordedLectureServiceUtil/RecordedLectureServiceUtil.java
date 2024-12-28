package com.yoganavi.lecture.recorded_lecture.service.recordedLectureServiceUtil;

import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.RecordedLectureChapter;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.RecordedLectureLikeRepository;
import com.yoganavi.lecture.recorded_lecture.dto.ChapterDto;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordedLectureServiceUtil {

    private final RecordedLectureLikeRepository recordedLectureLikeRepository;

    public LectureDto convertToDto(RecordedLecture lecture) {
        if (lecture == null) {
            throw new IllegalArgumentException("변환할 강의 정보 없음");
        }

        LectureDto dto = new LectureDto();
        dto.setRecordedId(lecture.getId());
        dto.setUserId(lecture.getUser().getUserId());
        dto.setNickname(lecture.getUser().getNickname());
        dto.setRecordTitle(lecture.getTitle());
        dto.setRecordContent(lecture.getContent());
        dto.setRecordThumbnail(lecture.getThumbnail());
        dto.setRecordThumbnailSmall(lecture.getThumbnailSmall());
        dto.setMyLike(false);

        List<ChapterDto> chapterDtos = new ArrayList<>();
        if (lecture.getChapters() != null) {
            for (RecordedLectureChapter chapter : lecture.getChapters()) {
                try {
                    ChapterDto chapterDto = new ChapterDto();
                    chapterDto.setId(chapter.getId());
                    chapterDto.setChapterTitle(chapter.getTitle());
                    chapterDto.setChapterDescription(chapter.getDescription());
                    chapterDto.setChapterNumber(chapter.getChapterNumber());
                    chapterDto.setRecordVideo(chapter.getVideoUrl());
                    chapterDtos.add(chapterDto);
                } catch (Exception e) {
                    log.warn("챕터 변환 중 오류 (해당 챕터 제외): chapterId={}, error={}",
                        chapter.getId(), e.getMessage());
                }
            }
        }
        dto.setRecordedLectureChapters(chapterDtos);

        return dto;
    }

    public void getLikeInformation(LectureDto dto, RecordedLecture lecture, Users user) {
        try {
            long likeCount = recordedLectureLikeRepository.countLikesByLectureId(lecture.getId());
            boolean isLiked = recordedLectureLikeRepository.existsByLectureAndUser(lecture, user);

            dto.setLikeCount(likeCount);
            dto.setMyLike(isLiked);
        } catch (DataAccessException e) {
            log.warn("좋아요 카운트 조회 실패: lectureId={}, error={}", lecture.getId(), e.getMessage());
            throw new ServiceException("좋아요 정보 조회 중 오류 발생", e);
        }
    }
}
