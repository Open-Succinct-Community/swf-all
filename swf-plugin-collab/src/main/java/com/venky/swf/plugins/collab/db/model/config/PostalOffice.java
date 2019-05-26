package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_FIELD("PIN_CODE")
@EXPORTABLE(false)
public interface PostalOffice extends Model {
    public Long getCountryId();
    public void setCountryId(Long id);
    public Country getCountry();

    public String getOfficeName();
    public void setOfficeName(String name);

    @Index
    @UNIQUE_KEY
    public String getPinCode();
    public void setPinCode(String code);

    @Enumeration("S.O,B.O,H.O")
    public String getOfficeType();
    public void setOfficeType(String type);

    @Enumeration("Delivery,Non-Delivery")
    public String getDeliveryStatus();
    public void setDeliveryStatus(String status);


    public String getDivisionName();
    public void setDivisionName(String name);


    public String getRegionName();
    public void setRegionName(String regionName);


    @Index
    public String getCircleName();
    public void setCircleName(String circleName);

    @Index
    public String getTaluk();
    public void setTaluk(String taluk);

    @Index
    public String getDistrict();
    public void setDistrict(String district);

    public String getStateName();
    public void setStateName(String stateName);

    @Index
    public Long getStateId();
    public void setStateId(Long id);
    public State getState();

    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();


}
