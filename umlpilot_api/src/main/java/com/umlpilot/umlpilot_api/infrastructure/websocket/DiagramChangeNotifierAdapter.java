package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.port.out.DiagramChangeNotifier;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link DiagramChangeNotifier} sobre {@link DiagramEventPublisher}.
 */
@Component
public class DiagramChangeNotifierAdapter implements DiagramChangeNotifier {

    private final DiagramEventPublisher eventPublisher;

    public DiagramChangeNotifierAdapter(DiagramEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void bulkChanged(String diagramId, String userId, String origin) {
        eventPublisher.diagramChanged(diagramId, userId, origin);
    }
}
