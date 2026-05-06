package com.hotpulse.service.hotspot;

import com.corundumstudio.socketio.SocketIOServer;
import com.hotpulse.dto.HotspotResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Socket.IO hotspot push service.
 * Manages the Netty-SocketIO server lifecycle and pushes new hotspot events
 * to connected frontend clients via the "/ws/hotspots" namespace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotSocketService {

    private final SocketIOServer socketIOServer;

    @PostConstruct
    public void start() {
        try {
            socketIOServer.start();
            log.info("Socket.IO server started on port {}", socketIOServer.getConfiguration().getPort());
        } catch (Exception e) {
            log.error("Failed to start Socket.IO server", e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            socketIOServer.stop();
            log.info("Socket.IO server stopped");
        } catch (Exception e) {
            log.warn("Error stopping Socket.IO server", e);
        }
    }

    /**
     * Push a newly generated hotspot to all connected frontend clients.
     */
    public void pushNewHotspot(HotspotResponse hotspot) {
        try {
            socketIOServer.getNamespace("/ws/hotspots")
                    .getBroadcastOperations()
                    .sendEvent("newHotspot", hotspot);
        } catch (Exception e) {
            log.warn("Failed to push hotspot via Socket.IO: {}", e.getMessage());
        }
    }
}
