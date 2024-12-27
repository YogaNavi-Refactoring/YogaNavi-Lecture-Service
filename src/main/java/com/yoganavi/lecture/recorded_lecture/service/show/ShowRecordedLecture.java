package com.yoganavi.lecture.recorded_lecture.service.show;

import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import java.util.List;

public interface ShowRecordedLecture {

    List<LectureDto> getMyLectures(Long userId);

    List<LectureDto> getLikeLectures(Long userId);

    LectureDto getLectureDetails(long recordedId, Long userId);

    List<LectureDto> getAllLectures(Long userId, int page, int size, String sort);
}
