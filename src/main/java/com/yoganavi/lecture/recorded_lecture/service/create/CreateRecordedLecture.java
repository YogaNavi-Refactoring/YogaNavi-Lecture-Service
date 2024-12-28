package com.yoganavi.lecture.recorded_lecture.service.create;

import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;

public interface CreateRecordedLecture {

    void saveLecture(LectureDto lectureDto);
}
