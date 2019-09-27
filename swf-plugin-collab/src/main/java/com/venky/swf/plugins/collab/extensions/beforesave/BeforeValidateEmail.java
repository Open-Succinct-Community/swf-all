package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.Email;

import java.util.regex.Pattern;

public class BeforeValidateEmail<E extends Email & Model> extends BeforeModelValidateExtension<E> {
    @Override
    public void beforeValidate(E model) {
        ModelReflector<E> reflector = ModelReflector.instance(getModelClass(this));
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
    }
}
