package com.venky.swf.plugins.collab.controller;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
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
        return generate(null);
    }

    @RequireLogin(false)
    public View generate(String input_algo) throws Exception{
        String algo = Crypt.KEY_ALGO ;
        int strength = 2048 ;
        if (input_algo != null){
            String[] parts = input_algo.split(":");
            if (parts.length != 2){
                throw new RuntimeException("Pass eg. Ed25519:256");
            }
            algo = parts[0];
            strength = Integer.valueOf(parts[1]);
        }
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();
        String[] pair = CryptoKey.generateKeyPair(algo,strength);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return IntegrationAdaptor.instance(getModelClass(), FormatHelper.getFormatClass(MimeType.APPLICATION_JSON)).createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));
    }

}
