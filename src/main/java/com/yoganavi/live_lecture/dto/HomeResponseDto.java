package com.yoganavi.live_lecture.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 홈 페이지 응답 DTO
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class HomeResponseDto {

    private Long liveId;
    private String nickname;
    private String profileImageUrl;
    private String profileImageUrlSmall;
    private String liveTitle;
    private String liveContent;
    private Long startTime;
    private Long endTime;
    private Long lectureDate;
    private String lectureDay;
    private Integer maxLiveNum;
    private Boolean teacher;
    private Boolean isOnAir;

    public boolean isTeacher() {
        return teacher;
    }

    public void setTeacher(boolean teacher) {
        this.teacher = teacher;
    }

    public HomeResponseDto(
        Long liveId,
        String nickname,
        String profileImageUrl,
        String profileImageUrlSmall,
        String liveTitle,
        String liveContent,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String dayOfWeek,
        Integer maxLiveNum,
        Boolean teacher,
        Boolean isOnAir
    ) {
        this.liveId = liveId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.profileImageUrlSmall = profileImageUrlSmall;
        this.liveTitle = liveTitle;
        this.liveContent = liveContent;
        this.startTime = startTime.toEpochSecond(ZoneOffset.UTC);
        this.endTime = endTime.toEpochSecond(ZoneOffset.UTC);
        this.lectureDate = startTime.toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        this.lectureDay = dayOfWeek.toUpperCase();
        this.maxLiveNum = maxLiveNum;
        this.teacher = teacher;
        this.isOnAir = isOnAir;
    }
}
