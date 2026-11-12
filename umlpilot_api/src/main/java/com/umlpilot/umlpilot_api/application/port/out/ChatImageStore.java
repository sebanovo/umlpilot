package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.application.dto.ChatImageData;

import java.util.Optional;

public interface ChatImageStore {

    void store(String messageId, byte[] image, String filename, String contentType);

    Optional<ChatImageData> load(String messageId);
}
