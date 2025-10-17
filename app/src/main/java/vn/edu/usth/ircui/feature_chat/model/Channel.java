package vn.edu.usth.ircui.feature_chat.model;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.feature_chat.data.Message;

/** Defines a Channel model with a name, a privacy flag (isPrivate),
 * a list of messages, and a list of participants (usernames). **/

public class Channel {
    private final String name;
    private final boolean isPrivate;
    private final List<Message> messages;
    private final List<String> participants;


    // General Chat
    public Channel(String name, boolean isPrivate) {
        this.name = name;
        this.isPrivate = isPrivate;
        this.messages = new ArrayList<>();
        this.participants = new ArrayList<>();
    }

    // Private chat
    public Channel (String name, boolean isPrivate, List<String> participants){
        this.name = name;
        this.isPrivate = isPrivate;
        this.participants = new ArrayList<>(participants);
        this.messages = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public List<String> getParticipants(){
        return participants;
    }
    public List<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
