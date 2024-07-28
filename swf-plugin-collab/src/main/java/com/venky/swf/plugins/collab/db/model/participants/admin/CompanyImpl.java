package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompanyImpl extends ModelImpl<Company>{

	public CompanyImpl(Company proxy) {
		super(proxy);
	}

	public Company getSelfCompany(){
		return getProxy();
	}


	public List<Long> getStaffUserIds(){
		Company company = getProxy();

		Select select  = new Select("DISTINCT users.id AS ID").from(Role.class,UserRole.class,User.class);
		select.add(String.format("""
							where users.company_id = %d and user_roles.user_id = users.id and roles.id = user_roles.role_id and ( roles.staff = true or roles.name = 'STAFF' )
							""",company.getId()));
		List<User> users = select.execute(User.class);
		return DataSecurityFilter.getIds(users);
	}

	public Company getCustomer(){
		return getProxy();
	}

	public Company getVendor(){
		return getProxy();
	}


	public Long getAnyUserId(){
		return null;
	}
	public void setAnyUserId(Long anyUserId) {

	}
	public User getAnyUser(){
		return null;
	}
}
