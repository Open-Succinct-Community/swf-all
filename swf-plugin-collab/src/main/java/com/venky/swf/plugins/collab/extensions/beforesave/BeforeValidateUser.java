package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.User;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressValidator;

import java.util.regex.Pattern;

public class BeforeValidateUser extends BeforeValidateAddress<User> {
    static {
        registerExtension(new BeforeValidateUser());
    }
    
    @Override
    public void beforeValidate(User user) {
        if (!ObjectUtil.isVoid(user.getName()) && user.getName().contains("@")){
            if (EmailAddressValidator.isValid(user.getName())) {
                if (ObjectUtil.isVoid(user.getEmail()) ||
                        !ObjectUtil.equals(user.getName(),user.getEmail())) {
                    user.setEmail(user.getName().toLowerCase());
                }
            }else {
                throw new RuntimeException("Please enter a valid email address!");
            }
        }
        super.beforeValidate(user);
    }
}
