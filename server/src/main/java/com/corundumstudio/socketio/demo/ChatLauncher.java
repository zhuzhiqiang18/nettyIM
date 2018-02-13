package com.corundumstudio.socketio.demo;

import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import java.util.*;

public class ChatLauncher {
    /**
     * 用户列表
     */
    public static Set<User> userOnline= new HashSet<User>();
    /**
     * 用户socket
     */
    public static Map<String,SocketIOClient> user = new HashMap<String,SocketIOClient>();
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration();
        config.setHostname("192.168.1.166");
        config.setPort(9092);

        final SocketIOServer server = new SocketIOServer(config);
        //消息
        server.addEventListener("chatevent", Message.class, new DataListener<Message>() {
            @Override
            public void onData(SocketIOClient client, Message data, AckRequest ackRequest) {
                data.setFromId(client.getSessionId().toString());
                client= user.get(data.getId());
                client.sendEvent("chatevent",data);

            }
        });
        //监听下线用户，并从用户列表中移出后广播所有用户更新在线列表
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
              //  System.out.println("onConnect");
            }
        });
        //下线
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                System.out.println(socketIOClient.getSessionId().toString()+"下线");
                userOnline.remove(new User().setId(socketIOClient.getSessionId().toString()));
                server.getBroadcastOperations().sendEvent("onLineList", JSONObject.toJSON(userOnline));
            }
        });
        //维护在线用户
        server.addEventListener("onLineList",ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                server.getBroadcastOperations().sendEvent("onLineList", JSONObject.toJSON(userOnline));
            }
        });
        //登录
        server.addEventListener("login",ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {

                userOnline.add(new User().setId(client.getSessionId().toString()).setUserName(data.getUserName()));
                user.put(client.getSessionId().toString(),client);
                data.setMessage(client.getSessionId().toString());
                client.sendEvent("login", data);
            }
        });

        server.start();

        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }

}
