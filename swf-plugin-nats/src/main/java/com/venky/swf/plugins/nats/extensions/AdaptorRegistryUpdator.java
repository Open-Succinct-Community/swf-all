package com.venky.swf.plugins.nats.extensions;

import com.venky.swf.plugins.nats.message.NatsAdaptor;

public class AdaptorRegistryUpdator {
    static {
       NatsAdaptor.registerAdaptor();
    }
}
