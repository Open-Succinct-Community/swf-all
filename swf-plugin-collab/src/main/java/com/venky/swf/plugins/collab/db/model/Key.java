package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_FIELD("ALIAS")
public interface Key extends Model {
    @UNIQUE_KEY
    public String getAlias();
    public void setAlias(String alias);

    @ENCRYPTED
    public String getPrivateKey();
    public void setPrivateKey(String key);

    public String getPublicKey();
    public void setPublicKey(String key);

    public static Key find(String alias){
        Key key = Database.getTable(Key.class).newRecord();
        key.setAlias(alias);
        key = Database.getTable(Key.class).find(key,false);
        return key;
    }
}
