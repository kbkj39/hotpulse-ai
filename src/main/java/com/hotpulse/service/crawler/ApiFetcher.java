package com.hotpulse.service.crawler;

import com.hotpulse.entity.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ApiFetcher {

    public List<CandidateItem> fetch(Source source, List<String> keywords) {
        // TODO: 根据具体 API 类型实现（如 NewsAPI、CryptoCompare 等）
        log.info("ApiFetcher: source={}, keywords={}", source.getName(), keywords);
        return Collections.emptyList();
    }
}
