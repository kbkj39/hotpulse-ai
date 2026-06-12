package com.hotpulse.service.hotspot;

import com.hotpulse.dto.DailyReportResponse;
import com.hotpulse.entity.DailyReport;
import com.hotpulse.entity.DailyReportStatus;
import com.hotpulse.repository.DailyReportRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyReportServiceTest {

    @Test
    void regenerateResetsExistingReportAndTriggersAsyncGeneration() {
        DailyReportRepository repository = mock(DailyReportRepository.class);
        DailyReportGenerator generator = mock(DailyReportGenerator.class);
        DailyReportService service = new DailyReportService(repository, generator);
        LocalDate date = LocalDate.of(2026, 6, 10);

        DailyReport report = new DailyReport();
        report.setReportDate(date);
        report.setStatus(DailyReportStatus.READY);
        report.setContent("old report");
        report.setHotspotCount(12);
        report.setGeneratedAt(Instant.parse("2026-06-10T12:00:00Z"));
        report.setErrorMessage("old error");

        when(repository.findByReportDate(date)).thenReturn(Optional.of(report));
        when(repository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReportResponse response = service.triggerRegenerate(date);

        assertThat(report.getStatus()).isEqualTo(DailyReportStatus.PENDING);
        assertThat(report.getContent()).isEqualTo("");
        assertThat(report.getHotspotCount()).isNull();
        assertThat(report.getGeneratedAt()).isNull();
        assertThat(report.getErrorMessage()).isNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getContent()).isEqualTo("");
        verify(generator).generateAsync(date);
    }

    @Test
    void regenerateCreatesPendingReportWithNonNullContent() {
        DailyReportRepository repository = mock(DailyReportRepository.class);
        DailyReportGenerator generator = mock(DailyReportGenerator.class);
        DailyReportService service = new DailyReportService(repository, generator);
        LocalDate date = LocalDate.of(2026, 6, 10);

        when(repository.findByReportDate(date)).thenReturn(Optional.empty());
        when(repository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReportResponse response = service.triggerRegenerate(date);

        assertThat(response.getReportDate()).isEqualTo("2026-06-10");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getContent()).isEqualTo("");
        verify(generator).generateAsync(date);
    }

    @Test
    void regenerateRejectsReportThatIsAlreadyGenerating() {
        DailyReportRepository repository = mock(DailyReportRepository.class);
        DailyReportGenerator generator = mock(DailyReportGenerator.class);
        DailyReportService service = new DailyReportService(repository, generator);
        LocalDate date = LocalDate.of(2026, 6, 10);

        DailyReport report = new DailyReport();
        report.setReportDate(date);
        report.setStatus(DailyReportStatus.GENERATING);
        report.setContent("");

        when(repository.findByReportDate(date)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.triggerRegenerate(date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already generating");

        verify(repository, never()).save(any(DailyReport.class));
        verify(generator, never()).generateAsync(any(LocalDate.class));
    }
}
