package com.yoganavi.lecture.recorded_lecture.service.delete;

import com.yoganavi.lecture.recorded_lecture.dto.DeleteDto;

public interface DeleteRecordedLecture {

    void deleteLectures(DeleteDto deleteDto, Long userId);
}
