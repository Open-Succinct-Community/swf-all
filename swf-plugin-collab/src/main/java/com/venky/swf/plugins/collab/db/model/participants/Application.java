package com.venky.swf.plugins.collab.db.model.participants;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;

public interface Application extends com.venky.swf.db.model.application.Application , CompanyNonSpecific {


    @UNIQUE_KEY("K1,K2")
    @IS_NULLABLE(false)
    public String getAppId();

    String getChangeSecret(); //Just for reordering in ui

}
