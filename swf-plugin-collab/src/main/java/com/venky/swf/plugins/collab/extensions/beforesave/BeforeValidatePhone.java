package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.Phone;

public class BeforeValidatePhone<P extends Phone& Model> extends BeforeModelValidateExtension<P> {
    @Override
    public void beforeValidate(P model) {
        model.setPhoneNumber(Phone.sanitizePhoneNumber(model.getPhoneNumber()));
    }
}
