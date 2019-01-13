package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;

import java.util.HashSet;
import java.util.Set;

public class UserImpl extends ModelImpl<User> {
    public UserImpl(){

    }
    public UserImpl(User user){
        super(user);
    }

    public Long getCompanyId(){
        for (UserCompany uc : getProxy().getUserCompanies()){
            return uc.getCompanyId();
        }
        return null;
    }
    public void setCompanyId(Long id){
        if (id == null){
            return ;
        }
        Set<Long> companyIds = new HashSet<>();
        getProxy().getUserCompanies().forEach(uc->companyIds.add(uc.getCompanyId()));
        if (!companyIds.contains(id)){
            UserCompany uc = Database.getTable(UserCompany.class).newRecord();
            uc.setUserId(getProxy().getId());
            uc.setCompanyId(id);
            uc.save();
        }
    }
}
