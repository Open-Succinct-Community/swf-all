package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.user.UserEmail;

public class BeforeValidateUserEmail extends BeforeValidateEmail<UserEmail> {
    static {
        registerExtension(new BeforeValidateUserEmail());
    }
}
