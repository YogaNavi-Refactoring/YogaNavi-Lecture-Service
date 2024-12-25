package com.yoganavi.lecture.live_lecture.service.history;

import com.yoganavi.lecture.live_lecture.dto.LectureHistoryDto;
import java.util.List;

public interface HistoryService {

    List<LectureHistoryDto> getHistory(Long userId, int page, int size);
}
