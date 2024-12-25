package com.yoganavi.lecture.live_lecture.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 화상 강의 생성 응답
 *
 */
@Setter
@Getter
public class LiveLectureCreateResponseDto {
    private String message;
    private Object data;
}
