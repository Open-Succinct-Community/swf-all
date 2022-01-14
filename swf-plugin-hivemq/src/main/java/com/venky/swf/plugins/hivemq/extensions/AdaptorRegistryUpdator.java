package com.venky.swf.plugins.hivemq.extensions;

import com.venky.swf.plugins.hivemq.message.HiveMqAdaptor;

public class AdaptorRegistryUpdator {
    static {
       HiveMqAdaptor.registerAdaptor();
    }
}
