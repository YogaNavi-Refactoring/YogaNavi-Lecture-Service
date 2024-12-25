package com.yoganavi.lecture.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeUtil {
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    public LocalDate toLocalDate(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli)
            .atZone(ZONE_SEOUL)
            .toLocalDate();
    }

    public LocalTime toLocalTime(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli)
            .atZone(ZONE_SEOUL)
            .toLocalTime();
    }

    public long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE_SEOUL)
            .toInstant()
            .toEpochMilli();
    }

    public long timeToEpochMilli(LocalTime time) {
        return time.toNanoOfDay() / 1_000_000;
    }
}
