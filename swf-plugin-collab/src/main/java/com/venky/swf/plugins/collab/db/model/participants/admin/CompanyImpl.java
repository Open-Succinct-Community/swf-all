package com.venky.swf.plugins.collab.db.model.participants.admin;

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

import java.util.List;

public class CompanyImpl extends ModelImpl<Company>{

	public CompanyImpl(Company proxy) {
		super(proxy);
	}

	public Company getSelfCompany(){
		return getProxy();
	}


	public List<Long> getStaffUserIds(){
		Company company = getProxy();

		Role staff = Role.getRole("STAFF");

		ModelReflector<User> ref = ModelReflector.instance(User.class);
		Expression where = new Expression(ref.getPool(), "COMPANY_ID", Operator.EQ, company.getId());
		Select select = new Select("ID").from(User.class).where(where);

		select.add(" and exists (select 1 from user_roles where role_id = " + staff.getId() + " and user_id = users.id )");

		List<User> users = select.execute();
		return DataSecurityFilter.getIds(users);
	}

	public Company getCustomer(){
		return getProxy();
	}

	public Company getVendor(){
		return getProxy();
	}

}
