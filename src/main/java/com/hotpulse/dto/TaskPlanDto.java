package com.hotpulse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskPlanDto {
    /** 意图分类："chat" 直接对话，"search" 触发搜索管线 */
    private String intent;
    private List<String> topics;
    private List<String> sources;
    private List<String> keywords;
    private String priority;
    private String timeRange;
}
