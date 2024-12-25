package com.yoganavi.lecture.live_lecture.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LectureHistoryDto {

    private Long liveId;
    private String nickname;
    private String profileImageUrlSmall;
    private String liveTitle;
    private Long startTime;
    private Long endTime;
    private Long lectureDate;
    private String lectureDay;

    public LectureHistoryDto(
        Long liveId,
        String nickname,
        String profileImageUrlSmall,
        String liveTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String dayOfWeek
    ) {
        this.liveId = liveId;
        this.nickname = nickname;
        this.profileImageUrlSmall = profileImageUrlSmall;
        this.liveTitle = liveTitle;
        this.startTime = startTime.toEpochSecond(ZoneOffset.UTC);
        this.endTime = endTime.toEpochSecond(ZoneOffset.UTC);
        this.lectureDate = startTime.toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        this.lectureDay = dayOfWeek.toUpperCase();
    }

    @Override
    public String toString() {
        return "LectureHistory{" +
            "liveId=" + liveId +
            ", nickname='" + nickname + '\'' +
            ", liveTitle='" + liveTitle + '\'' +
            ", startTime=" + LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
            ", endTime=" + LocalDateTime.ofEpochSecond(endTime, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
            ", lectureDate=" + LocalDateTime.ofEpochSecond(lectureDate, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
            ", lectureDay='" + lectureDay + '\'' +
            '}';
    }
}
