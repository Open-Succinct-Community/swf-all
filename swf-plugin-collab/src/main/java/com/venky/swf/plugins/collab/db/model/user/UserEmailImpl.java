package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class UserEmailImpl extends EmailImpl<UserEmail> {
    public UserEmailImpl(UserEmail email) {
        super(email);
    }
    public Long getCompanyId(){
        String email = getProxy().getEmail();
        Long companyId = null;
        if (!ObjectUtil.isVoid(email)){
            if (email.indexOf('@')>0){
                String domain = email.substring(email.indexOf('@')+1);
                Select select = new Select().from(Company.class);
                Expression where = new Expression(select.getPool(),"DOMAIN_NAME" , Operator.EQ, domain);
                List<Company> companies = select.where(where).execute(2);
                if (companies.size() == 1){
                    companyId = companies.get(0).getId();
                }
            }
        }
        return companyId;
    }
}
