package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.routing.Config;

public class BeforeSaveUser extends BeforeSaveAddress<User> {
    static {
        registerExtension(new BeforeSaveUser());
    }

    @Override
    protected boolean isOkToSetLocationAsync() {
        return  Config.instance().getBooleanProperty("swf.collab.user.geo.assign.async",false);
    }
}
