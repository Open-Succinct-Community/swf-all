package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelCreateExtension;
import com.venky.swf.db.model.application.ApplicationPublicKey;

import java.util.UUID;

public class BeforeCreateApplicationPublicKey extends BeforeModelCreateExtension<ApplicationPublicKey> {
    static {
        registerExtension(new BeforeCreateApplicationPublicKey());
    }
    @Override
    public void beforeCreate(ApplicationPublicKey model) {
        if (ObjectUtil.isVoid(model.getKeyId())){
            model.setKeyId(UUID.randomUUID().toString());
        }
    }
}
