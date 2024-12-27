package com.yoganavi.lecture.recorded_lecture.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureDto {

    private Long recordedId;
    private Long userId;
    private String nickname;
    private String recordTitle;
    private String recordContent;
    private String recordThumbnail;
    private String recordThumbnailSmall;
    private List<ChapterDto> recordedLectureChapters;
    private Long likeCount;
    private Boolean myLike;
    private String creationStatus;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    // 상세 조회용 (RecordedServiceImpl의 getMyLectures, getLikeLectures 메서드)
    public LectureDto(Long recordedId, Long userId, String nickname, String recordTitle,
        String recordContent, String recordThumbnail, String recordThumbnailSmall,
        Long likeCount, LocalDateTime createdDate, LocalDateTime lastModifiedDate, Boolean myLike) {
        this.recordedId = recordedId;
        this.userId = userId;
        this.nickname = nickname;
        this.recordTitle = recordTitle;
        this.recordContent = recordContent;
        this.recordThumbnail = recordThumbnail;
        this.recordThumbnailSmall = recordThumbnailSmall;
        this.likeCount = likeCount;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
        this.myLike = myLike;
    }
}