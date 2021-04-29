package com.venky.swf.plugins.templates.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.templates.db.model.User;

public class BeforeValidateUser extends BeforeModelValidateExtension<User> {
    static {
        registerExtension(new BeforeValidateUser());
    }
    @Override
    public void beforeValidate(User model) {
        model.setPhoneNumber(User.sanitizePhoneNumber(model.getPhoneNumber()));
    }
}
