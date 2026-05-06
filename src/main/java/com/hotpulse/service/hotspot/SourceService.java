package com.hotpulse.service.hotspot;

import com.hotpulse.entity.Source;
import com.hotpulse.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceRepository sourceRepository;

    public List<Source> getSources() {
        return sourceRepository.findAll();
    }

    public Source createSource(Source source) {
        return sourceRepository.save(source);
    }

    public Source updateSource(Long id, Source updated) {
        Source existing = sourceRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("信息源不存在: " + id));
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setBaseUrl(updated.getBaseUrl());
        existing.setEnabled(updated.getEnabled());
        existing.setReputationScore(updated.getReputationScore());
        return sourceRepository.save(existing);
    }

    public Source setEnabled(Long id, boolean enabled) {
        Source existing = sourceRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("信息源不存在: " + id));
        existing.setEnabled(enabled);
        return sourceRepository.save(existing);
    }

    public boolean deleteSource(Long id) {
        if (!sourceRepository.existsById(id)) return false;
        sourceRepository.deleteById(id);
        return true;
    }
}
