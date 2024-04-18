package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.Email;

import java.util.regex.Pattern;

public class BeforeValidateEmail<E extends Email & Model> extends BeforeModelValidateExtension<E> {
    @Override
    public void beforeValidate(E model) {
        String email = model.getEmail();
        if (!ObjectUtil.isVoid(email) && model.getRawRecord().isFieldDirty("EMAIL")){
            Email.validate(email);
        }
    }
}
