package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.Key;
import com.venky.swf.views.View;
import com.venky.core.security.Crypt;

import java.security.KeyPair;
import java.util.Arrays;

public class KeysController extends VirtualModelController<Key> {
    public KeysController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public View generate(){
        Key key = Database.getTable(Key.class).newRecord();
        KeyPair pair = Crypt.getInstance().generateKeyPair(Crypt.KEY_ALGO,2048);
        key.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
        key.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
        return getIntegrationAdaptor().createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));
    }
}
