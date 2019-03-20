package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.regex.Pattern;

public class BeforeValidateUser extends BeforeModelValidateExtension<User> {
    static {
        registerExtension(new BeforeValidateUser());
    }

    @Override
    public void beforeValidate(User model) {
        if (!ObjectUtil.isVoid(model.getPhoneNumber())){
            if (model.getRawRecord().isFieldDirty("PHONE_NUMBER")){
                String phoneNumber = model.getPhoneNumber();
                int length = model.getPhoneNumber().length();
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
                    model.setPhoneNumber(phoneNumber);
                }else {
                    throw new RuntimeException("Phone number invalid e.g. +911234567890");
                }
            }
        }
    }
}
