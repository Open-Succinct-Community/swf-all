package com.venky.swf.plugins.collab.db.model;

import com.venky.core.security.Crypt;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;

import java.security.KeyPair;

@HAS_DESCRIPTION_FIELD("ALIAS")
public interface CryptoKey extends Model {
    @UNIQUE_KEY
    public String getAlias();
    public void setAlias(String alias);

    @ENCRYPTED
    @COLUMN_SIZE(4096)
    public String getPrivateKey();
    public void setPrivateKey(String key);

    @COLUMN_SIZE(4096)
    public String getPublicKey();
    public void setPublicKey(String key);

    public static CryptoKey find(String alias){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();
        key.setAlias(alias);
        key = Database.getTable(CryptoKey.class).find(key,false);
        return key;
    }

    public static String[] generateKeyPair(String algo, int strength){
        KeyPair pair = Crypt.getInstance().generateKeyPair(algo,strength);
        return new String[] {
                Crypt.getInstance().getBase64Encoded(pair.getPrivate()),
                Crypt.getInstance().getBase64Encoded(pair.getPublic())
        };
    }
}
