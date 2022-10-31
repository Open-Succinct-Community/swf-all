package com.venky.swf.db.model.application;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface Application extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    public String getAppId();
    public void setAppId(String id);


    @PASSWORD
    @HIDDEN
    @PROTECTION
    public String getSecret();
    public void setSecret(String secret);

    @IS_VIRTUAL
    @COLUMN_SIZE(60)
    @PASSWORD
    public String getChangeSecret();
    @IS_VIRTUAL
    public void setChangeSecret(String secret);


    public String getEncryptedSecret(String unEncryptedSecret);

    public List<WhiteListIp> getWhiteListIps();
    public List<ApplicationEvent> getApplicationEvents();
    public List<ApplicationPublicKey> getApplicationPublicKeys();


    public String getHashingAlgorithm();
    public void setHashingAlgorithm(String hashingAlgorithm);

    public String getHashingAlgorithmCommonName();
    public void setHashingAlgorithmCommonName(String hashingAlgorithm);

    public String getSigningAlgorithm();
    public void setSigningAlgorithm(String signingAlgorithm);

    public String getSigningAlgorithmCommonName();
    public void setSigningAlgorithmCommonName(String signingAlgorithm);

    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "60000")
    public long getSignatureLifeMillis();
    public void setSignatureLifeMillis(long signatureLifeMillis);

    public String getHeaders();
    public void setHeaders(String headers);



}
