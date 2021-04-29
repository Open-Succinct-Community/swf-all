package com.venky.swf.plugins.templates.db.model;


import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.plugins.templates.db.model.alerts.Alert;
import com.venky.swf.plugins.templates.db.model.alerts.Device;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public interface User extends com.venky.swf.plugins.mail.db.model.User {

    @CONNECTED_VIA("USER_ID")
    public List<Alert> getAlerts();

    @HIDDEN
    public List<Device> getDevices();

    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public boolean isNotificationEnabled();
    public void setNotificationEnabled(boolean enabled);

    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public Boolean isEmailNotificationEnabled();
    public void setEmailNotificationEnabled(boolean enabled);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isWhatsAppNotificationEnabled();
    public void setWhatsAppNotificationEnabled(boolean enabled);

    @WATERMARK("e.g +911234567890")
    @Index
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    public static String sanitizePhoneNumber(String phoneNumber){
        if (ObjectUtil.isVoid(phoneNumber)) {
            return  null;
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

