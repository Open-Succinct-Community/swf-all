package com.venky.swf.db.model.application;

import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

public class ApplicationImpl extends ModelImpl<Application> {
    public ApplicationImpl(Application proxy){
        super(proxy);
    }
    public String getChangeSecret(){
        return "";
    }

    public void setChangeSecret(String secret){
        if (!ObjectUtil.isVoid(secret)){
            getProxy().setSecret(getEncryptedSecret(secret));
        }
    }

    public String getEncryptedSecret(String unEncryptedSecret){
        String encryptedSecret = unEncryptedSecret;
        if (!ObjectUtil.isVoid(unEncryptedSecret) && Config.instance().shouldPasswordsBeEncrypted()){
            Application app = getProxy();
            if (app.getReflector().isVoid(app.getCreatedAt())){
                app.setCreatedAt(app.getReflector().getNow());
            }
            long time = app.getCreatedAt().getTime();
            String salt = time + "--" + app.getAppId() + "--" ;
            encryptedSecret = Encryptor.encrypt(unEncryptedSecret + "--" + salt);
        }
        return encryptedSecret;
    }

}
