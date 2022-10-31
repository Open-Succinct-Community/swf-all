package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

public interface ApplicationPublicKey extends Model {
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    public static final String PURPOSE_SIGNING = "SIGNING";
    public static final String PURPOSE_ENCRYPTION = "ENCRYPTION";

    @Enumeration(PURPOSE_ENCRYPTION+","+PURPOSE_SIGNING)
    public String getPurpose();
    public void setPurpose(String purpose);


    @Enumeration("Ed25519,X35519,RSA")
    public String getAlgorithm();
    public void setAlgorithm(String algorithm);

    @WATERMARK("Enter base 64 encoded pem format public key")
    public String getPublicKey();
    public void setPublicKey(String publicKey);

    @UNIQUE_KEY("KeyId")
    public String getKeyId();
    public void setKeyId(String keyId);


    public static ApplicationPublicKey find(String keyId){
        ApplicationPublicKey applicationPublicKey = Database.getTable(ApplicationPublicKey.class).newRecord();
        applicationPublicKey.setKeyId(keyId);
        return  Database.getTable(ApplicationPublicKey.class).find(applicationPublicKey,false);
    }
}
