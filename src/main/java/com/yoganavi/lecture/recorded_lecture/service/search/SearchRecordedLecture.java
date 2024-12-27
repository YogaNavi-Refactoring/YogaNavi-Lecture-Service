package com.yoganavi.lecture.recorded_lecture.service.search;

import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import java.util.List;

public interface SearchRecordedLecture {

    List<LectureDto> searchLectures(Long userId, String keyword, String sort, int page,
        int size, boolean title, boolean content);
}
