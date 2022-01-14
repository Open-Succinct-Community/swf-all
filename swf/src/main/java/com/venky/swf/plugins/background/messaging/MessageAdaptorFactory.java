package com.venky.swf.plugins.background.messaging;

import com.venky.cache.Cache;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.routing.Config;

import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MessageAdaptorFactory implements Extension {
    private static volatile MessageAdaptorFactory sSoleInstance;

    //private constructor.
    private MessageAdaptorFactory(){

        //Prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static MessageAdaptorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (MessageAdaptorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new MessageAdaptorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected MessageAdaptorFactory readResolve() {
        return getInstance();
    }


    private Map<String,MessageAdaptor> map = new HashMap<>();
    public void registerMessageAdaptor(MessageAdaptor adaptor){
        map.put(adaptor.getProvider(),adaptor);
    }
    public MessageAdaptor getDefaultMessageAdaptor(){
        String defaultProvider = Config.instance().getProperty("swf.message.provider.default");
        if (defaultProvider == null){
            if (map.size() == 1){
                defaultProvider = map.keySet().iterator().next();
            }
        }
        if (defaultProvider == null){
            throw  new RuntimeException("Default message provider not configured!");
        }
        return getMessageAdaptor(defaultProvider);
    }
    public MessageAdaptor getMessageAdaptor(String provider){
        MessageAdaptor adaptor = map.get(provider);
        if (adaptor == null){
            throw new RuntimeException("No such provider");
        }
        return adaptor;
    }
    public void dispose(){
        Set<String> keySet = new HashSet<>(map.keySet());
        keySet.forEach(k->{
            map.remove(k).disconnect();
        });
    }

    static {
        Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown",getInstance());
    }

    @Override
    public void invoke(Object... context) {
        dispose();
    }
}
