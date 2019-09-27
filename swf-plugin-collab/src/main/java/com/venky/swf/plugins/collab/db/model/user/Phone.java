package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.ui.WATERMARK;

import java.util.regex.Pattern;

public interface Phone extends OtpEnabled{
    @WATERMARK("e.g +911234567890")
    @IS_NULLABLE(false)
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    public static String sanitizePhoneNumber(String phoneNumber){

        if (!ObjectUtil.isVoid(phoneNumber)){
            int length = phoneNumber.length();
            if (length == 10){
                phoneNumber = ("+91"+phoneNumber);
            }else if (length == 12){
                phoneNumber = ("+"+phoneNumber);
            }
            if (phoneNumber.length() != 13){
                throw new RuntimeException("Phone number invalid e.g. +911234567890");
            }
            Pattern pattern = Pattern.compile("\\+[0-9]+");
            if (!pattern.matcher(phoneNumber).matches()){
                throw new RuntimeException("Phone number invalid e.g. +911234567890");
            }
        }
        return phoneNumber;
    }
}
