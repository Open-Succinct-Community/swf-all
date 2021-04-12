package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.model.Model;

public interface Key extends Model {
    @ENCRYPTED
    public String getPrivateKey();
    public void setPrivateKey(String key);

    public String getPublicKey();
    public void setPublicKey(String key);

}
