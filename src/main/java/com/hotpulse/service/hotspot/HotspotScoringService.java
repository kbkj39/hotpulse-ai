package com.hotpulse.service.hotspot;

import com.hotpulse.entity.Hotspot;
import org.springframework.stereotype.Service;

@Service
public class HotspotScoringService {

    private static final double IMPORTANCE_WEIGHT  = 0.5;
    private static final double TIMELINESS_WEIGHT  = 0.3;
    private static final double REPUTATION_WEIGHT  = 0.2;

    /**
     * hotScore = importanceScore * 0.5 + timelinessScore * 0.3 + reputationScore * 0.2
     */
    public double computeHotScore(double importanceScore, long ageHours, double sourceReputation) {
        double timelinessScore = Math.max(0, 1.0 - ageHours / 72.0); // 72h 内线性衰减
        return IMPORTANCE_WEIGHT * importanceScore
                + TIMELINESS_WEIGHT * timelinessScore
                + REPUTATION_WEIGHT * sourceReputation;
    }
}
