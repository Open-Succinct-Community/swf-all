package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.core.util.ObjectUtil;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface Address extends GeoLocation {
    public String getAddressLine1();
    public void setAddressLine1(String line1);

    public String getAddressLine2();
    public void setAddressLine2(String line2);

    public String getAddressLine3();
    public void setAddressLine3(String line3);

    public String getAddressLine4();
    public void setAddressLine4(String line4);

    @COLUMN_NAME("ADDRESS_LINE_4")
    String getLandmark();
    void setLandmark(String landmark);
    
    
    @IS_NULLABLE
    @PARTICIPANT(value = "CITY", redundant = true)
    @OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.participants.admin.AddressCitySelectionProcessor")
    public Long getCityId();
    public void setCityId(Long cityId);
    public City getCity();

    @IS_NULLABLE
    @PARTICIPANT(value = "STATE",  redundant = true)
    @OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.participants.admin.AddressStateSelectionProcessor")
    public Long getStateId();
    public void setStateId(Long stateId);
    public State getState();

    @IS_NULLABLE
    @PARTICIPANT(value = "COUNTRY",  redundant = true)

    public Long getCountryId();
    public void setCountryId(Long countryId);
    public Country getCountry();

    public Long getPinCodeId();
    public void setPinCodeId(Long pincodeId);
    public PinCode getPinCode();


    public String getEmail();
    public void setEmail(String emailId);

    @WATERMARK("e.g +911234567890")
    @Index
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    @WATERMARK("e.g +911234567890")
    @Index
    public String getAlternatePhoneNumber();
    public  void setAlternatePhoneNumber(String phoneNumber);


    public static String[] getAddressFields(){
        return new String[]{"ADDRESS_LINE_1","ADDRESS_LINE_2","ADDRESS_LINE_3","ADDRESS_LINE_4","CITY_ID","STATE_ID","COUNTRY_ID","PIN_CODE_ID"};
    }
    public static List<String[]> getGeoCodingFields(){
        return new ArrayList<>(){{
            add(new String[]{"NAME","ADDRESS_LINE_1","ADDRESS_LINE_2","ADDRESS_LINE_3","ADDRESS_LINE_4"});
            add(new String[]{"LANDMARK"});
        }};
        
    }
    
    public static <F extends Model & Address,T extends Model & Address> void copy(F from, T to){
       for (String f : getAddressFields()){
           to.getRawRecord().put(f,from.getRawRecord().get(f));
       }
       to.setLat(from.getLat());
       to.setLng(from.getLng());
       to.setEmail(from.getEmail());
       to.setPhoneNumber(from.getPhoneNumber());
       to.setAlternatePhoneNumber(from.getAlternatePhoneNumber());
    }

    public static <M extends Model&Address> boolean isAddressVoid(M oAddress){
        boolean addressFieldsVoid = true;
        for (String field : getAddressFields()){
            addressFieldsVoid = addressFieldsVoid && oAddress.getReflector().isVoid(oAddress.getReflector().get(oAddress,field));
            if (!addressFieldsVoid){
                break;
            }
        }
        return addressFieldsVoid;

    }

    public static <M extends Model & Address> boolean isAddressChanged(M oAddress){
        Set<String> dirtyFields = oAddress.getRawRecord().getDirtyFields();
        boolean addressFieldsChanged = false;
        for (String field : getAddressFields()){
            addressFieldsChanged = addressFieldsChanged || dirtyFields.contains(field);
            if (addressFieldsChanged){
                break;
            }
        }
        return addressFieldsChanged;
    }
    public static <M extends Model & Address, A extends  Model&Address> M find(A address,Class<M> modelClass){
        ModelReflector<M> ref = ModelReflector.instance(modelClass);

        Expression where = new Expression(ref.getPool(), Conjunction.AND);
        for (String field : getAddressFields()){
            Object value = address.getReflector().get(address,field);
            if (!ObjectUtil.isVoid(value)){
                where.add(new Expression(ref.getPool(),field, Operator.EQ, value));
            }else{
                where.add(new Expression(ref.getPool(),field,Operator.EQ));
            }
        }

        List<M> objects = new Select().from(modelClass).where(where).execute();
        M object = null;
        if (!objects.isEmpty()){
            object = objects.get(0);
        }
        return object;
    }
}
