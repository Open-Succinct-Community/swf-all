package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.user.User;

public class BeforeSaveUser extends BeforeSaveAddress<User> {
    static {
        registerExtension(new BeforeSaveUser());
    }

    @Override
    protected boolean isOkToSetLocationAsync() {
        return false;
    }
}
