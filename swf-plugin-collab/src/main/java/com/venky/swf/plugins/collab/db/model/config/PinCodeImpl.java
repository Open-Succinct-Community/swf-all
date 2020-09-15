package com.venky.swf.plugins.collab.db.model.config;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.parser.SQLExpressionParser.EQ;

import java.util.ArrayList;
import java.util.List;

public class PinCodeImpl extends ModelImpl<PinCode> {
    public PinCodeImpl(){
        super();
    }
    public PinCodeImpl(PinCode pinCode){
        super(pinCode);
    }

    public void loadPostalOffice(){
        if (postalOffice == null){
            PinCode pinCode = getProxy();
            if (!ObjectUtil.isVoid(pinCode.getPinCode())){
                List<PostalOffice> postalOfficeList = new Select().from(PostalOffice.class).where(new Expression(ModelReflector.instance(PostalOffice.class).getPool(),"PIN_CODE", Operator.EQ, pinCode.getPinCode())).execute();
                if (!postalOfficeList.isEmpty()){
                    postalOffice = postalOfficeList.get(0);
                }
            }
        }
    }
    private PostalOffice postalOffice =  null;

    public Long getStateId() {
        loadPostalOffice();
        if ( postalOffice != null){
            return postalOffice.getStateId();
        }
        return null;
    }
    public void setStateId(Long id){

    }

    public Long getCityId() {
        loadPostalOffice();
        if (postalOffice != null){
            if (postalOffice.getCityId() == null){
                for (String name : new String[]{postalOffice.getTaluk(),postalOffice.getRegionName(), postalOffice.getDistrict()}){
                    if (!ObjectUtil.isVoid(name)){
                        List<City> cities = new Select().from(City.class).where(
                                new Expression(ModelReflector.instance(City.class).getPool(),"NAME", Operator.EQ, name))
                                .execute(1);
                        if (!cities.isEmpty()){
                            return cities.get(0).getId();
                        }
                    }
                }
            }
        }
        return null;
    }
    public void setCityId(Long id){

    }
}
