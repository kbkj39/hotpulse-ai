package com.hotpulse.common;

public final class AgentConstants {

    private AgentConstants() {}

    public static final String PLANNER_AGENT   = "PlannerAgent";
    public static final String SEARCHER_AGENT  = "SearcherAgent";
    public static final String CRAWLER_AGENT   = "CrawlerAgent";
    public static final String ANALYZER_AGENT  = "AnalyzerAgent";
    public static final String AGGREGATOR_AGENT = "AggregatorAgent";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DONE    = "DONE";
    public static final String STATUS_FAILED  = "FAILED";
}
