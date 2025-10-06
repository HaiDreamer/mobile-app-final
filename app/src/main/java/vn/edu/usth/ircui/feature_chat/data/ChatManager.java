package vn.edu.usth.ircui.feature_chat.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.usth.ircui.feature_chat.model.Channel;

public class ChatManager {
    private final Map<String, Channel> channels;
    private String currentUser;

    public ChatManager(String currentUser){
        this.currentUser = currentUser;
        this.channels = new HashMap<>();
        channels.put("#General", new Channel("#General", false));
    }

    public void setCurrentUser(String username){
        this.currentUser = username;
    }

    // General chat
    public void sendMessage(String channelName, String content) {
        Channel channel = channels.get(channelName);
        if (channel == null){
            channel = new Channel (channelName, false);
            channels.put(channelName, channel);
        }

        Message message = new Message(currentUser, content, true);
        channel.addMessage(message);
    }

    public Channel getChannel(String channelName){
        return channels.get(channelName);
    }

    public Map<String, Channel> getAllChannels(){
        return channels;
    }

    // Private chat
    private String getPrivateChannelID(String user1, String user2){
        return user1.compareTo(user2) < 0 ? user1 + "-" + user2 : user2 + "-" + user1;
    }

    public Channel getOrCreatePrivateChat(String toUser){
        String channelID = getPrivateChannelID(currentUser, toUser);
        Channel channel = channels.get(channelID);

        if(channel == null){
            List<String> participants = List.of(currentUser, toUser);
            channel = new Channel (channelID, true, participants);
            channels.put(channelID, channel);
        }
        return channel;
    }

    public void sendPrivateMessage(String toUser, String content){
        Channel privateChannel = getOrCreatePrivateChat(toUser);
        Message message = new Message(currentUser, content, true);
        privateChannel.addMessage(message);
    }

    public List<Channel> getPrivateChannels(){
        List<Channel> privates = new ArrayList<>();
        for (Channel c : channels.values()){
            if (c.isPrivate() && c.getParticipants().contains(currentUser)){
                privates.add(c);
            }
        }
        return privates;
    }
}
