package com.venky.swf.extensions;

import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.db.model.application.api.EventHandler;

public class EventHandlerExtension extends ModelOperationExtension<EventHandler> {
    static {
        registerExtension(new EventHandlerExtension());
    }

    @Override
    protected void beforeValidate(EventHandler instance) {
        super.beforeValidate(instance);
        if (instance.getApplicationId() == null){
            if (instance.getEndPointId() != null) {
                instance.setApplicationId(instance.getEndPoint().getApplicationId());
            }
        }
    }
}
