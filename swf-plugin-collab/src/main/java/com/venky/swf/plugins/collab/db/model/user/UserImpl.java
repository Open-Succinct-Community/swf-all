package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;

import com.venky.swf.plugins.collab.db.model.config.Role;
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


}
