package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

import java.sql.Timestamp;

public interface ApplicationPublicKey extends Model {

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
    public void setKeyId(String keyId);

    @IS_NULLABLE
    public Timestamp getValidFrom();
    public void setValidFrom(Timestamp from);

    @IS_NULLABLE
    public Timestamp getValidUntil();
    public void setValidUntil(Timestamp until);


    public static ApplicationPublicKey find(String purpose, String keyId){
        ApplicationPublicKey applicationPublicKey = Database.getTable(ApplicationPublicKey.class).newRecord();
        applicationPublicKey.setPurpose(purpose);
        applicationPublicKey.setKeyId(keyId);
        return  Database.getTable(ApplicationPublicKey.class).find(applicationPublicKey,false);
    }
}
