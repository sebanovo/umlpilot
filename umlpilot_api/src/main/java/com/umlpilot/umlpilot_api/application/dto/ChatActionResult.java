package com.umlpilot.umlpilot_api.application.dto;

import com.umlpilot.umlpilot_api.domain.model.ChatAction;

public record ChatActionResult(String action, String entity, String id, String result) {

    public static ChatActionResult from(ChatAction action) {
        return new ChatActionResult(action.action(), action.entity(),
                action.id() == null ? "" : action.id(),
                action.result() == null ? "" : action.result());
    }
}
