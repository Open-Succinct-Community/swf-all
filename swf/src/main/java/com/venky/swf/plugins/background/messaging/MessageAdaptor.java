package com.venky.swf.plugins.background.messaging;

import com.venky.swf.routing.Config;
import io.cloudevents.CloudEvent;

import java.util.HashMap;
import java.util.Map;

public interface MessageAdaptor {
    public String getProvider();

    default MessageQueue getDefaultQueue(){
        return getMessageQueue(new HashMap<String,String>(){{
            put("host", Config.instance().getProperty(String.format("swf.message.%s.host",getProvider()),"localhost"));
            put("port",Config.instance().getProperty(String.format("swf.message.%s.port",getProvider()), "1883"));
            put("user",Config.instance().getProperty(String.format("swf.message.%s.user",getProvider())));
            put("password",Config.instance().getProperty(String.format("swf.message.%s.password",getProvider())));
        }});
    }
    public MessageQueue getMessageQueue(Map<String,String> connectionParams);

    interface MessageQueue  {
        public void publish(String topic,CloudEvent event);
        public CloudEvent receive (String topic, long timeOutMillis, boolean unsubscribeAfterReceipt);
        public void subscribe(String topic, CloudEventHandler handler );
    }

    interface CloudEventHandler{
        public void handle(String topic,CloudEvent event,SubscriptionHandle subscriptionHandle);
    }

    interface SubscriptionHandle {
        public void unsubscribe();
    }

    public String getSeparatorToken();
    public String getSingleLevelWildCard();
    public String getMultiLevelWildCard();

}
