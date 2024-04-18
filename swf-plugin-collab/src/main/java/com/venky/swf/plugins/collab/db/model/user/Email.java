package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;
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

    @IS_VIRTUAL
    public String getDomain();


}
