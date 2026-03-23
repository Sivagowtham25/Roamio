package com.example.roamio.models;

/**
 * ChatMessage
 * Represents a single message in the AI trip planner chat.
 */
public class ChatMessage {

    public enum Sender { USER, AI }

    private final String text;
    private final Sender sender;

    public ChatMessage(String text, Sender sender) {
        this.text   = text;
        this.sender = sender;
    }

    public String getText()   { return text; }
    public Sender getSender() { return sender; }
    public boolean isUser()   { return sender == Sender.USER; }
}
