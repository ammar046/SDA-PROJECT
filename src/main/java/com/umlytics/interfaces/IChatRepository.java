package com.umlytics.interfaces;

import com.umlytics.domain.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface IChatRepository {
    void save(ChatMessage m);

    List<ChatMessage> findByProject(UUID projectId);

    void delete(UUID id);
}
