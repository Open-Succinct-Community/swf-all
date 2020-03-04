package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;

import com.venky.swf.plugins.collab.db.model.config.Role;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserImpl extends ModelImpl<User> {
    public UserImpl(){

    }
    public UserImpl(User user){
        super(user);
    }


    public boolean isStaff(){
        List<Long> roleIds = getProxy().getUserRoles().stream().map(userRole -> userRole.getRoleId()).collect(Collectors.toList());
        List<Role> roles = new Select().from(Role.class).where(new Expression(ModelReflector.instance(Role.class).getPool(),"ID", Operator.IN,roleIds.toArray())).execute();
        boolean isStaff= false;
        for (Role role : roles){
            if (role.getName().equalsIgnoreCase("STAFF") || role.isStaff()){
                isStaff = true;
                break;
            }
        }
        return isStaff;
    }


    public com.venky.swf.db.model.User getSelfUser(){
        return getProxy();
    }


    public List<Long> getCompanyIds(){
        List<Long> ret = new SequenceSet<>();
        User u = getProxy();
        if (u.getCompanyId() != null){
            ret.add(u.getCompanyId());
            ret.addAll(u.getCompany().getCustomers().stream().map(r->r.getCustomerId()).collect(Collectors.toList()));
            ret.addAll(u.getCompany().getVendors().stream().map(r->r.getVendorId()).collect(Collectors.toList()));
            ret.addAll(u.getCompany().getCreatedCompanies().stream().map(c->c.getId()).collect(Collectors.toList()));
        }
        for (com.venky.swf.db.model.UserEmail userEmail : u.getUserEmails()){
            com.venky.swf.plugins.collab.db.model.user.UserEmail ue = userEmail.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.UserEmail.class);
            if (ue.isValidated()){
                Long companyId = ue.getCompanyId();
                if (!getReflector().isVoid(companyId )){
                    ret.add(companyId);
                }
            }
        }
        return ret;
    }

    public List<Company> getCompanies(){
        ModelReflector<Company> companyModelReflector = ModelReflector.instance(Company.class);
        return new Select().from(Company.class).where(new Expression(companyModelReflector.getPool(),"ID",Operator.IN,getCompanyIds())).execute();
    }
}
