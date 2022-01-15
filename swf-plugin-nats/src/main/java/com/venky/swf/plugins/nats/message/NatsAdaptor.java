package com.venky.swf.plugins.nats.message;

import com.venky.swf.plugins.background.messaging.MessageAdaptor;
import com.venky.swf.plugins.background.messaging.MessageAdaptorFactory;
import com.venky.swf.routing.Config;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Subscription;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NatsAdaptor implements MessageAdaptor {
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
        connect();
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

    Connection nc = null;

    @Override
    public String getProvider() {
        return getClass().getSimpleName().replaceAll("Adaptor","").toLowerCase();
    }

    @Override
    public void publish(String topic, CloudEvent event) {
        try {
            nc.publish(topic, EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).serialize(event));
            nc.flush(Duration.ZERO);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public CloudEvent receive(String topic, long timeOutMillis, boolean unsubscribeAfterReceipt) {
        Subscription subscription = nc.subscribe(topic);
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
        Dispatcher dispatcher = nc.createDispatcher(msg -> handler.handle(topic,EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(msg.getData())));
        dispatcher.subscribe(topic);
    }

    @Override
    public void connect() {
        synchronized (this){
            if (nc == null){
                Options options = new Options.Builder().server(Config.instance().getProperty("swf.message.nats.url")).
                        pingInterval(Duration.ofSeconds(20)).maxPingsOut(5).noEcho().maxReconnects(5)
                        .build();
                try {
                    nc = Nats.connect(options);
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @Override
    public void disconnect() {
        synchronized (this) {
            if (nc != null) {
                try {
                    nc.flush(Duration.ZERO);
                    nc.close();
                } catch (InterruptedException | TimeoutException e) {
                    //
                }
            }
        }

    }
}
