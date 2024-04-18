package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.util.CompanyFinder;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class UserEmailImpl extends EmailImpl<UserEmail> {
    public UserEmailImpl(UserEmail email) {
        super(email);
    }
    public Long getCompanyId(){
        Company company = getCompany();
        if (company.getRawRecord().isNewRecord()){
            return null;
        }else {
            return company.getId();
        }
    }
    public void setCompanyId(Long companyId){

    }
    public Company getCompany(){
        String domain = getProxy().getDomain();
        Company company = Database.getTable(Company.class).newRecord();
        company.setDomainName(domain);
        company = Database.getTable(Company.class).getRefreshed(company,false);
        if (company.getRawRecord().isNewRecord()){
            company.setName(domain);
        }
        return company;
    }

}
