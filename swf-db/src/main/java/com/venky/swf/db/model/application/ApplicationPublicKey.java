package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

import java.sql.Timestamp;

public interface ApplicationPublicKey extends Model {

    @IS_NULLABLE(false)
    @Index
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    public static final String PURPOSE_SIGNING = "SIGNING";
    public static final String PURPOSE_ENCRYPTION = "ENCRYPTION";

    @Enumeration(PURPOSE_ENCRYPTION+","+PURPOSE_SIGNING)

    @UNIQUE_KEY("KeyId")
    public String getPurpose();
    public void setPurpose(String purpose);


    @Enumeration("Ed25519,X25519,RSA")
    public String getAlgorithm();
    public void setAlgorithm(String algorithm);

    @WATERMARK("Enter base 64 encoded pem format public key")
    public String getPublicKey();
    public void setPublicKey(String publicKey);

    @UNIQUE_KEY("KeyId")
    public String getKeyId();
    public void     setKeyId(String keyId);

    @IS_NULLABLE
    public Timestamp getValidFrom();
    public void setValidFrom(Timestamp from);

    @IS_NULLABLE
    public Timestamp getValidUntil();
    public void setValidUntil(Timestamp until);

    public static ApplicationPublicKey find(String purpose, String keyId){
        return find(purpose,keyId, ApplicationPublicKey.class);
    }
    public static <T extends ApplicationPublicKey> T find(String purpose, String keyId, Class<T> clazz){
        T applicationPublicKey = Database.getTable(clazz).newRecord();
        applicationPublicKey.setPurpose(purpose);
        applicationPublicKey.setKeyId(keyId);
        return  Database.getTable(clazz).find(applicationPublicKey,false);
    }

    @IS_VIRTUAL
    boolean isExpired();


    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isVerified();
    public void setVerified(boolean verified);

}
