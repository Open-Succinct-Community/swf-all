package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import javax.xml.crypto.Data;
import java.util.List;

@HAS_DESCRIPTION_FIELD("PIN_CODE")
@ORDER_BY("PIN_CODE")
@EXPORTABLE(false)
public interface PinCode extends Model {
    @UNIQUE_KEY
    @Index
    public String getPinCode();
    public void setPinCode(String pinCode);

    public static PinCode find(String pincode){
        List<PinCode> pinCodeList =new Select().from(PinCode.class).where(new Expression(ModelReflector.instance(PinCode.class).getPool(),"PIN_CODE", Operator.EQ, pincode)).execute();
        if (!pinCodeList.isEmpty()){
            return pinCodeList.get(0);
        }else {
            PinCode pinCode = Database.getTable(PinCode.class).newRecord();
            pinCode.setPinCode(pincode);
            pinCode.save();
        }
        return null;
    }

    @IS_NULLABLE
    @IS_VIRTUAL
    @EXPORTABLE(false)
    public Long getStateId();
    public void setStateId(Long id);
    public State getState();

    @IS_NULLABLE
    @IS_VIRTUAL
    @EXPORTABLE(false)
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

}
