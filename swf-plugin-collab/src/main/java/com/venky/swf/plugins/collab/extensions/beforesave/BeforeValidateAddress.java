package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.regex.Pattern;

public class BeforeValidateAddress<M extends Address & Model> extends BeforeModelValidateExtension<M> {
    static {
        registerExtension(new BeforeValidateAddress());
    }

    @Override
    public void beforeValidate(M model) {
        ModelReflector<User> reflector = model.getReflector();
        for (String field : new String[]{"PHONE_NUMBER", "ALTERNATE_PHONE_NUMBER"}){
            String phoneNumber = reflector.get(model,field);

            if (!reflector.isVoid(phoneNumber) && model.getRawRecord().isFieldDirty(field)){
                int length = phoneNumber.length();
                if (length == 10){
                    phoneNumber = ("+91"+phoneNumber);
                }else if (length == 12){
                    phoneNumber = ("+"+phoneNumber);
                }
                if (phoneNumber.length() != 13){
                    throw new RuntimeException("Phone number invalid e.g. +911234567890");
                }
                Pattern pattern = Pattern.compile("\\+[0-9]+");
                if (pattern.matcher(phoneNumber).matches()){
                    reflector.set(model,field,phoneNumber);
                }else {
                    throw new RuntimeException("Phone number invalid e.g. +911234567890");
                }
            }
        }
    }
}
