package com.venky.swf.db.model;

import com.venky.core.security.Crypt;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.application.ApplicationPublicKey;

import java.security.KeyPair;

@HAS_DESCRIPTION_FIELD("ALIAS")
@MENU("Security")
public interface CryptoKey extends Model {
    @Index
    @UNIQUE_KEY
    public String getAlias();
    public void setAlias(String alias);

    public static final String PURPOSE_SIGNING = ApplicationPublicKey.PURPOSE_SIGNING;
    public static final String PURPOSE_ENCRYPTION = ApplicationPublicKey.PURPOSE_ENCRYPTION;

    @UNIQUE_KEY
    public String getPurpose();
    public void setPurpose(String purpose);

    public String getAlgorithm();
    public void setAlgorithm(String algorithm);

    @ENCRYPTED
    @COLUMN_SIZE(4096)
    public String getPrivateKey();
    public void setPrivateKey(String key);

    @COLUMN_SIZE(4096)
    public String getPublicKey();
    public void setPublicKey(String key);

    @PARTICIPANT
    public Long getCreatorUserId();

    public static CryptoKey find(String alias,String purpose){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();
        key.setAlias(alias);
        key.setPurpose(purpose);
        key = Database.getTable(CryptoKey.class).getRefreshed(key,false);
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
