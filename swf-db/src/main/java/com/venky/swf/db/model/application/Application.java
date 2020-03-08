package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.io.Reader;
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


    public String getEncryptedSecret(String unencyptedSecret);



}
