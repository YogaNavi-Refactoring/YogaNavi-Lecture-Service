package com.yoganavi.lecture.recorded_lecture.service.like;

public interface LikeRecordedLecture {

    boolean toggleLike(Long recordedId, Long userId);
}
