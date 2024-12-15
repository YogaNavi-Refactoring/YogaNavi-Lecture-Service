package com.yoganavi.live_lecture.service.home;

import com.yoganavi.live_lecture.dto.HomeResponseDto;
import java.util.List;

public interface HomeService {

    List<HomeResponseDto> getHomeData(Long userId, int page, int size);

    boolean updateLiveState(Long liveId, Boolean isOnAir);
}
