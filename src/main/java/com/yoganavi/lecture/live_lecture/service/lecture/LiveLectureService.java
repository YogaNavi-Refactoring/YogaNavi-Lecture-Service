package com.yoganavi.lecture.live_lecture.service.lecture;

import com.yoganavi.lecture.live_lecture.dto.LiveLectureInfoDto;
import com.yoganavi.lecture.live_lecture.dto.LiveLectureResponseDto;
import java.util.List;

public interface LiveLectureService {

    void createLiveLecture(LiveLectureInfoDto liveLectureInfoDto);

    List<LiveLectureResponseDto> getLiveLecturesByUserId(Long userId);

    boolean isLectureOwner(Long liveId, Long userId);

    void updateLiveLecture(LiveLectureInfoDto liveLectureInfoDto);

    void deleteLiveLectureById(Long liveId);

    LiveLectureResponseDto getLiveLectureById(Long liveId);
}
