package com.hotpulse.service.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Playwright 动态页面抓取器（用于 JS 渲染页面）
 * 目前提供基础框架，实际使用时需要安装浏览器：playwright install chromium
 */
@Slf4j
@Component
public class PlaywrightFetcher {

    public String fetchContent(String url) {
        try (com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create()) {
            try (com.microsoft.playwright.Browser browser = playwright.chromium().launch()) {
                com.microsoft.playwright.BrowserContext context = browser.newContext();
                com.microsoft.playwright.Page page = context.newPage();
                page.navigate(url);
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
                String content = page.innerText("body");
                context.close();
                return content;
            }
        } catch (Exception e) {
            log.error("Playwright fetch failed for url: {}", url, e);
            return "";
        }
    }
}
