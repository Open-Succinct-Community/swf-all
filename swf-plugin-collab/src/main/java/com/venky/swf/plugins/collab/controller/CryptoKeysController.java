package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.views.View;
import com.venky.core.security.Crypt;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.security.KeyPair;
import java.util.Arrays;

public class CryptoKeysController extends ModelController<CryptoKey> {
    public CryptoKeysController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public View generate() throws Exception{
        String algo = Crypt.KEY_ALGO;
        int strength = 2048 ;
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();
        String[] pair = CryptoKey.generateKeyPair(algo,strength);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return getIntegrationAdaptor().createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));
    }
}
