package com.umlytics.domain;

import java.util.ArrayList;
import java.util.List;

public class ProjectContext {
    private Project project;
    private UMLDiagram currentDiagram;
    private List<ChatMessage> chatHistory = new ArrayList<>();

    public String buildContextPrompt() {
        String projectName = project != null ? project.getName() : "UnknownProject";
        String diagramTitle = currentDiagram != null ? currentDiagram.getTitle() : "UntitledDiagram";
        return "Project: " + projectName + ", Diagram: " + diagramTitle + ", ChatMessages: " + chatHistory.size();
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public UMLDiagram getCurrentDiagram() {
        return currentDiagram;
    }

    public void setCurrentDiagram(UMLDiagram currentDiagram) {
        this.currentDiagram = currentDiagram;
    }

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }
}
