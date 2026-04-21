package com.umlytics.interfaces;

import com.umlytics.domain.ChatMessage;
import java.util.List;

public interface IChatRepository {
    void save(ChatMessage m);
    List<ChatMessage> findByProject(int projectId);
    void delete(int id);
}
