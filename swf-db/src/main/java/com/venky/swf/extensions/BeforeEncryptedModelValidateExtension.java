package com.venky.swf.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.util.SharedKeys;

import java.util.List;

public class BeforeEncryptedModelValidateExtension extends BeforeModelValidateExtension<Model> {
    static {
        registerExtension(new BeforeEncryptedModelValidateExtension());
    }
    @Override
    public void beforeValidate(Model model) {
        List<String> encryptedFields = model.getReflector().getEncryptedFields();
        if (encryptedFields.isEmpty()){
            return;
        }
        for (String f: encryptedFields){
            String value = model.getReflector().get(model,f);

            if (model.getRawRecord().isFieldDirty(f) && !model.getReflector().isVoid(value)){
                model.getReflector().set(model,f, SharedKeys.getInstance().encrypt(value));
            }
        }

    }
}
