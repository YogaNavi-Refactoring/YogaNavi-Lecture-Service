package com.yoganavi.live_lecture.service.history;

import com.yoganavi.live_lecture.dto.LectureHistoryDto;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface HistoryService {

    List<LectureHistoryDto> getHistory(Long userId, int page, int size);
}
