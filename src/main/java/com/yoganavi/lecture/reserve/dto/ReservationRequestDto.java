package com.yoganavi.lecture.reserve.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReservationRequestDto {

    private Long liveId;
    private String liveTitle;
    private String liveContent;
    private Instant startDate;
    private Instant endDate;
    private Instant startTime;
    private Instant endTime;
    private Integer maxLiveNum;
    private String availableDay;
    private Instant regDate;
    private Long userId;
}
