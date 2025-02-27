package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.regex.Pattern;

public class BeforeValidateAddress<M extends Address & Model> extends BeforeModelValidateExtension<M> {


    @Override
    public void beforeValidate(M model) {
        ModelReflector<M> reflector = model.getReflector();
        for (String field : new String[]{"PHONE_NUMBER", "ALTERNATE_PHONE_NUMBER"}){
            if (!reflector.getFields().contains(field)){
                continue;
            }
            String phoneNumber = Phone.sanitizePhoneNumber(reflector.get(model,field));
            reflector.set(model,field,phoneNumber);
        }

        String email = model.getEmail();
        if (!reflector.isVoid(email) && model.getRawRecord().isFieldDirty("EMAIL")){
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                    "[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                    "A-Z]{2,7}$";

            Pattern pat = Pattern.compile(emailRegex);
            if (!pat.matcher(email).matches()){
                throw new RuntimeException("Email is invalid!");
            }
        }
        if (model.getPinCodeId()  != null) {
            PinCode pinCode = model.getPinCode();
            
            if (ObjectUtil.isVoid(model.getCityId())) {
                model.setCityId(pinCode.getCityId());
            }
            if (!ObjectUtil.isVoid(pinCode.getStateId())) {
                model.setStateId(pinCode.getStateId()); //Ensure state matches pincode.
            }
        }
        if (model.getCityId() != null ) {
            if (model.getStateId() == null) {
                model.setStateId(model.getCity().getStateId());
            }else if (!ObjectUtil.equals(model.getStateId() , model.getCity().getStateId())){
                throw new RuntimeException("City not in state!");
            }
        }
        if (model.getStateId() != null ) {
            Long stateCountryId =  model.getState().getCountryId();
            model.setCountryId(stateCountryId);
        }
        
    }
}
