package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.ExactLength;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.config.PinCode;

public interface Address extends GeoLocation {
    public String getAddressLine1();
    public void setAddressLine1(String line1);

    public String getAddressLine2();
    public void setAddressLine2(String line2);

    public String getAddressLine3();
    public void setAddressLine3(String line3);

    public String getAddressLine4();
    public void setAddressLine4(String line4);

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

    @RegEx("\\+[0-9]+") //Ensures that it starts with + and all other characters are numbers.
    @ExactLength(13) // Ensures that user types in 13 characters in all in a phone field.
    @WATERMARK("e.g +911234567890")
    @Index
    public String getAlternatePhoneNumber();
    public  void setAlternatePhoneNumber(String phoneNumber);
}
