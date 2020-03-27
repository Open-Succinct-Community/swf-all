package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;

@IS_VIRTUAL
public interface Key extends Model {
    public String getPrivateKey();
    public void setPrivateKey(String key);

    public String getPublicKey();
    public void setPublicKey(String key);

}
