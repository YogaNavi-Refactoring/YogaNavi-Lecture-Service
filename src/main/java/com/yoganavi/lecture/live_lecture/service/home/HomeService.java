package com.yoganavi.lecture.live_lecture.service.home;

import com.yoganavi.lecture.live_lecture.dto.HomeResponseDto;
import java.util.List;

public interface HomeService {

    List<HomeResponseDto> getHomeData(Long userId, int page, int size);

    boolean updateLiveState(Long liveId, Boolean isOnAir);
}
