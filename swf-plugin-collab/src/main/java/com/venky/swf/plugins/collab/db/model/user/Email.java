package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.model.reflection.ModelReflector;

import java.util.regex.Pattern;

public interface Email extends OtpEnabled {
    @IS_NULLABLE(false)
    public String getEmail();
    public void setEmail(String email);

    public static void validate(String email){
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (!pat.matcher(email).matches()){
            throw new RuntimeException("Email is invalid!");
        }
    }

}
