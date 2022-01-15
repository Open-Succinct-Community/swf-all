package com.venky.swf.plugins.hivemq.message;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient.Mqtt5Publishes;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder.Send;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.messaging.MessageAdaptor;
import com.venky.swf.plugins.background.messaging.MessageAdaptorFactory;
import com.venky.swf.routing.Config;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiveMqAdaptor implements MessageAdaptor {
    public static void registerAdaptor() {
        MessageAdaptorFactory.getInstance().registerMessageAdaptor(getInstance());
    }
    @Override
    public String getProvider() {
        return getClass().getSimpleName().replaceAll("Adaptor","").toLowerCase();
    }

    private static volatile HiveMqAdaptor sSoleInstance;

    //private constructor.
    private HiveMqAdaptor(){

        //Prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
        loadClient();
    }

    public static HiveMqAdaptor getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (HiveMqAdaptor.class) {
                if (sSoleInstance == null) sSoleInstance = new HiveMqAdaptor();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected HiveMqAdaptor readResolve() {
        return getInstance();
    }


    private Mqtt5Client client = null;

    public void loadClient() {
        if (client == null){
            synchronized (this){
                if (client == null){
                    client = MqttClient.builder().identifier(UUID.randomUUID().toString()).
                            serverHost(Config.instance().getProperty("swf.message.hivemq.host")).useMqttVersion5().
                            build();
                }
            }
        }
    }
    public void connect(){
        if (client != null ){
            Send<Mqtt5ConnAck> connAckSend = client.toBlocking().connectWith();
            String user = Config.instance().getProperty("swf.message.hivemq.auth.userid");
            String password = Config.instance().getProperty("swf.message.hivemq.auth.password");
            if (!ObjectUtil.isVoid(user) && !ObjectUtil.isVoid(password)){
                connAckSend.simpleAuth().
                        username(user).
                        password(password.getBytes())
                        .applySimpleAuth();
            }
            connAckSend.send();
            logger.log(Level.INFO, "Connected to broker");
        }
    }
    public void disconnect(){
        if ( client != null) {
            client.toBlocking().disconnect();
        }
    }

    private static final Logger logger = Config.instance().getLogger(HiveMqAdaptor.class.getName());

    @Override
    public void publish(String topic, CloudEvent event) {
        client.toAsync().publishWith().topic(topic).payload(EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).serialize(event)).send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.SEVERE, "Error publishing", throwable);
                    } else {
                        logger.log(Level.INFO, "published message to topic.." + topic);
                    }
                });
    }

    /**
     * Subscribe async.
     *
     * @param topic    the topic
     * @param handler the callback
     */
    @Override
    public void subscribe(String topic, CloudEventHandler handler) {
        client.toAsync().subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE)
                .callback(serializedEvent -> handler.handle(topic, EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).
                        deserialize(serializedEvent.getPayloadAsBytes()))).send().
                whenComplete((suback, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error subscribing", throwable);
            } else {
                logger.log(Level.INFO, "subscribed message from topic.." + topic);
            }
        });
    }
    /**
     * Subscribe blocking.
     *
     * @param topic the topic
     * @return the cloud event
     * @throws InterruptedException the interrupted exception
     */
    public CloudEvent receive(String topic, long timeOutMillis, boolean unsubscribeOnCompletion) {
        try (final Mqtt5Publishes publishes = client.toBlocking().publishes(MqttGlobalPublishFilter.ALL)) {
            client.toBlocking().subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE).send();
            Optional<Mqtt5Publish> message = publishes.receive(timeOutMillis, TimeUnit.MILLISECONDS);
            return message.map(mqtt5Publish -> EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(mqtt5Publish.getPayloadAsBytes())).orElse(null);
        } catch (InterruptedException e) {
            return null;
        }finally {
            if (unsubscribeOnCompletion) {
                client.toAsync().unsubscribeWith().topicFilter(topic).send().whenComplete((mqtt5UnsubAck, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.SEVERE, "Error un subscribing", throwable);
                    } else {
                        logger.log(Level.INFO, "Unsubscribed from topic.." + topic);
                    }
                });
            }
        }

    }
}
