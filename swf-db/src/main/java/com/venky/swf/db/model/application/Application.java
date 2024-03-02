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
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.api.EventHandler;
import com.venky.swf.db.model.application.api.EndPoint;

import java.io.Reader;
import java.util.List;

@HAS_DESCRIPTION_FIELD("APP_ID")
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
    public List<ApplicationPublicKey> getApplicationPublicKeys();



    @Enumeration(",BLAKE2B-512")
    @IS_NULLABLE
    public String getHashingAlgorithm();
    public void setHashingAlgorithm(String hashingAlgorithm);


    /*
    Not used.
    public String getHashingAlgorithmCommonName();
    public void setHashingAlgorithmCommonName(String hashingAlgorithm);
    */

    @IS_NULLABLE
    @Enumeration(",Ed25519")
    public String getSigningAlgorithm();
    public void setSigningAlgorithm(String signingAlgorithm);

    /*
    public String getSigningAlgorithmCommonName();
    public void setSigningAlgorithmCommonName(String signingAlgorithm);
    */


    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "60000")
    public long getSignatureLifeMillis();
    public void setSignatureLifeMillis(long signatureLifeMillis);

    public String getHeaders();
    public void setHeaders(String headers);

    public String getIndustryClassificationCode();
    public void setIndustryClassificationCode(String industryClassificationCode);


    public List<EventHandler> getEventHandlers(); //
    public List<EndPoint> getEndPoints();

}
