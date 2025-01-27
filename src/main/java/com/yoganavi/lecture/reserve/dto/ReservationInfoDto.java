package com.yoganavi.lecture.reserve.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationInfoDto {

    private Long liveId;
    private String liveTitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String availableDay;
}