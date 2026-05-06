package com.hotpulse.dto;

import com.hotpulse.entity.MonitorKeyword;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonitorKeywordCreateResponse {
    private final MonitorKeyword keyword;
    /** 若 triggerNow=true 则为触发的执行 ID，否则为 null。*/
    private final Long executionId;
}
