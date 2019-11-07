package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.ui.WATERMARK;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

public interface Phone extends OtpEnabled{
    @WATERMARK("e.g +911234567890")
    @IS_NULLABLE(false)
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    public static String sanitizePhoneNumber(String phoneNumber){
        if (ObjectUtil.isVoid(phoneNumber)) {
            return  "";
        }

        StringBuilder ret = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(phoneNumber,"\r\f\t\n -+:()");
        while (tokenizer.hasMoreTokens()){
            ret.append(tokenizer.nextToken());
        }
        while (ret.charAt(0) == '0'){
            ret.deleteCharAt(0);
        }
        if (phoneNumber.charAt(0) == '+'){
            ret.insert(0,'+');
        }else {
            int length = ret.length();
            if (length == 10){
                ret.insert(0,"+91");
            }else if (length == 12){
                ret.insert(0,"+");
            }

            if (ret.length() != 13){
                throw new RuntimeException("Phone number invalid e.g. +911234567890");
            }
        }

        Pattern pattern = Pattern.compile("\\+[0-9]+");
        if (!pattern.matcher(ret.toString()).matches()){
            throw new RuntimeException("Phone number invalid e.g. +911234567890");
        }

        return ret.toString();
    }
}
