package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

import java.util.regex.Pattern;

public class BeforeValidateCompany extends ModelOperationExtension<Company> {
    static {
        registerExtension(new BeforeValidateCompany());
    }
    
    @Override
    protected void beforeValidate(Company instance) {
        super.beforeValidate(instance);
        if (!ObjectUtil.isVoid(instance.getVirtualPaymentAddress())){
            validate(instance.getVirtualPaymentAddress());
        }
    }
    
    public static void validate(String vpa){
        String vpaRegEx = "^[a-zA-Z0-9_+&*-]+" +
                "(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z]{2,7}$)";
        
        Pattern pat = Pattern.compile(vpaRegEx);
        if (!pat.matcher(vpa).matches()){
            throw new RuntimeException("VPA/UPI is invalid!");
        }
    }
    
}
