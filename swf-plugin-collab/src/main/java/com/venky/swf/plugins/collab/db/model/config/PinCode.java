package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_FIELD("PIN_CODE")
@ORDER_BY("PIN_CODE")
@EXPORTABLE(false)
public interface PinCode extends Model {
    @UNIQUE_KEY
    @Index
    public String getPinCode();
    public void setPinCode(String pinCode);
}
