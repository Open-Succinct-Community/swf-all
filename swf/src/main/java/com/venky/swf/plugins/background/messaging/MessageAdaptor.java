package com.venky.swf.plugins.background.messaging;

import io.cloudevents.CloudEvent;

public interface MessageAdaptor {
    public String getProvider();
    public void publish(String topic,CloudEvent event);
    public CloudEvent receive (String topic, long timeOutMillis, boolean unsubscribeAfterReceipt);
    public void subscribe(String topic, CloudEventHandler handler );

    interface CloudEventHandler{
        public void handle(String topic,CloudEvent event);
    }

}
