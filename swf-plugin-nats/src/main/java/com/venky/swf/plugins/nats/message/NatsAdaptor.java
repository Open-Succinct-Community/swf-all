package com.venky.swf.plugins.nats.message;

import com.venky.cache.Cache;
import com.venky.swf.plugins.background.messaging.MessageAdaptor;
import com.venky.swf.plugins.background.messaging.MessageAdaptorFactory;
import com.venky.swf.routing.Config;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.AuthHandler;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Options.Builder;
import io.nats.client.Subscription;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class NatsAdaptor implements MessageAdaptor , Closeable {
    public static void registerAdaptor() {
        MessageAdaptorFactory.getInstance().registerMessageAdaptor(getInstance());
    }
    private static volatile NatsAdaptor sSoleInstance;

    //private constructor.
    private NatsAdaptor(){
        //Prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static NatsAdaptor getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (NatsAdaptor.class) {
                if (sSoleInstance == null) sSoleInstance = new NatsAdaptor();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected NatsAdaptor readResolve() {
        return getInstance();
    }

    private final Map<Map<String,String>,NatsMessageQueue> queueMap = new Cache<Map<String, String>, NatsMessageQueue>() {
        @Override
        protected NatsMessageQueue getValue(Map<String, String> connectionParams) {
            return new NatsMessageQueue(connectionParams);
        }
    };
    public MessageQueue getMessageQueue(Map<String,String> connectionParams){
        return queueMap.get(connectionParams);
    }

    public static class NatsMessageQueue implements MessageQueue {
        Map<String,String> connectionParams = new HashMap<>();
        public NatsMessageQueue(Map<String,String> connectionParams){
            if (connectionParams != null){
                this.connectionParams.putAll(connectionParams);
            }
            connect();
        }
        Connection published = null;
        Connection subscribed = null;
        @Override
        public void publish(String topic, CloudEvent event) {
            try {
                published.publish(topic, EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).serialize(event));
                published.flush(Duration.ZERO);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

        @Override
        public CloudEvent receive(String topic, long timeOutMillis, boolean unsubscribeAfterReceipt) {
            Subscription subscription = subscribed.subscribe(topic);
            try {
                Message message = subscription.nextMessage(timeOutMillis);
                return EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(message.getData());
            }catch (Exception ex) {
                throw new RuntimeException(ex);
            }finally {
                if (unsubscribeAfterReceipt){
                    subscription.unsubscribe();
                }
            }

        }

        @Override
        public void subscribe(String topic, CloudEventHandler handler) {
            Dispatcher dispatcher = subscribed.createDispatcher();
            dispatcher.subscribe(topic,msg -> {
                try {
                    handler.handle(topic, EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(msg.getData()),()->dispatcher.unsubscribe(topic));
                }catch (Exception ex){
                    Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Exception processing subscription ",ex);
                }
            });
        }
        public void connect() {
            synchronized (this){
                StringBuilder url = new StringBuilder();
                url.append("nats://");
                url.append(connectionParams.getOrDefault("host","localhost"));
                url.append(":");
                url.append(connectionParams.getOrDefault("port","4222"));

                Builder builder = new Options.Builder().server(url.toString()).
                        pingInterval(Duration.ofSeconds(20)).maxPingsOut(5).noEcho().maxReconnects(5);

                if (connectionParams.get("user") != null && connectionParams.get("password") != null){
                    builder.userInfo(connectionParams.get("user"),connectionParams.get("password"));
                }
                Options options = builder.build();

                if (published == null){
                    try {
                        published = Nats.connect(options);
                    }catch (Exception ex){
                        throw new RuntimeException(ex);
                    }
                }
                if (subscribed == null){
                    try {
                        subscribed = Nats.connect(options);
                    }catch (Exception ex){
                        throw new RuntimeException(ex);
                    }
                }


            }
        }
        public void close() {
            synchronized (this) {
                if (published != null) {
                    try {
                        published.flush(Duration.ZERO);
                        published.close();
                    } catch (InterruptedException | TimeoutException e) {
                        //
                    }finally {
                        published = null;
                    }
                }
                if (subscribed != null) {
                    try {
                        subscribed.flush(Duration.ZERO);
                        subscribed.close();
                    } catch (InterruptedException | TimeoutException e) {
                        //
                    }finally {
                        subscribed = null;
                    }
                }
            }

        }

    }


    @Override
    public String getProvider() {
        return getClass().getSimpleName().replaceAll("Adaptor","").toLowerCase();
    }

    public void close(){
        queueMap.forEach((connectionParams,q)->q.close());
    }
    public String getSeparatorToken(){
        return ".";
    }
    public String getSingleLevelWildCard(){
        return "*";
    }
    public String getMultiLevelWildCard(){
        return ">";
    }

}
