package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.user.UserPhone;

public class BeforeValidateUserPhone extends BeforeValidatePhone<UserPhone> {
    static {
        registerExtension(new BeforeValidateUserPhone());
    }
}
