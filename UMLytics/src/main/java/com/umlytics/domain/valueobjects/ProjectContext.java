package com.umlytics.domain.valueobjects;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides context to AI prompts: current project + diagram + chat history.
 */
public class ProjectContext {
    private Project        project;
    private UMLDiagram     currentDiagram;
    private List<ChatMessage> chatHistory = new ArrayList<>();

    public ProjectContext() {}

    public ProjectContext(Project project, UMLDiagram currentDiagram, List<ChatMessage> chatHistory) {
        this.project        = project;
        this.currentDiagram = currentDiagram;
        this.chatHistory    = chatHistory != null ? chatHistory : new ArrayList<>();
    }

    /** Builds a rich context string for the LLM prompt. */
    public String buildContextPrompt() {
        StringBuilder sb = new StringBuilder();
        if (project != null) {
            sb.append("Project: ").append(project.getName()).append("\n");
            sb.append("Description: ").append(project.getDescription()).append("\n\n");
        }
        if (currentDiagram != null) {
            sb.append("Current Diagram: ").append(currentDiagram.getTitle()).append("\n");
            sb.append("Classes: ");
            sb.append(currentDiagram.getClasses().stream()
                    .map(c -> c.getName()).collect(Collectors.joining(", ")));
            sb.append("\n\n");
        }
        if (!chatHistory.isEmpty()) {
            sb.append("Recent chat history:\n");
            int start = Math.max(0, chatHistory.size() - 5);
            for (int i = start; i < chatHistory.size(); i++) {
                ChatMessage m = chatHistory.get(i);
                sb.append(m.getSender()).append(": ").append(m.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    public Project     getProject()         { return project; }
    public void        setProject(Project p) { this.project = p; }
    public UMLDiagram  getCurrentDiagram()  { return currentDiagram; }
    public void        setCurrentDiagram(UMLDiagram d) { this.currentDiagram = d; }
    public List<ChatMessage> getChatHistory() { return chatHistory; }
    public void        setChatHistory(List<ChatMessage> h) { this.chatHistory = h; }
}
