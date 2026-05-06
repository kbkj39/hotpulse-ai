package com.hotpulse.service.agent;

import com.hotpulse.common.AgentConstants;
import com.hotpulse.dto.TaskPlanDto;
import com.hotpulse.skill.PlanSearchSkill;
import com.hotpulse.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerAgent {

    private final PlanSearchSkill planSearchSkill;
    private final AgentExecutionTracker tracker;

    public TaskPlanDto plan(Long executionId, String query) {
        tracker.recordStep(executionId, AgentConstants.PLANNER_AGENT, AgentConstants.STATUS_RUNNING,
                "正在理解查询意图并生成任务计划...", null);
        try {
            SkillResult<TaskPlanDto> result = planSearchSkill.execute(query);
            if (result.isOk()) {
                TaskPlanDto plan = result.data();
                String msg = String.format("已生成任务计划：%s，%d 个信息源，%d 个关键词",
                        String.join(",", plan.getTopics() != null ? plan.getTopics() : java.util.List.of()),
                        plan.getSources() != null ? plan.getSources().size() : 0,
                        plan.getKeywords() != null ? plan.getKeywords().size() : 0);
                tracker.recordStep(executionId, AgentConstants.PLANNER_AGENT, AgentConstants.STATUS_DONE, msg, plan);
                return plan;
            } else {
                String errorMsg = "任务计划生成失败: " + result.error();
                tracker.recordStep(executionId, AgentConstants.PLANNER_AGENT, AgentConstants.STATUS_FAILED,
                        errorMsg, null);
                throw new RuntimeException(errorMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("PlannerAgent failed", e);
            String errorMsg = "PlannerAgent 异常: " + e.getMessage();
            tracker.recordStep(executionId, AgentConstants.PLANNER_AGENT, AgentConstants.STATUS_FAILED,
                    errorMsg, null);
            throw new RuntimeException(errorMsg, e);
        }
    }
}
