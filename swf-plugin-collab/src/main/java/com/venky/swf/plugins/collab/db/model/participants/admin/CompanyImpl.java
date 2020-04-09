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
		ModelReflector<Role> roleRef = ModelReflector.instance(Role.class);
		List<Role> staffRoles = new Select().from(Role.class).where(new Expression(roleRef.getPool(),Conjunction.OR).
				add(new Expression(roleRef.getPool(),"NAME",Operator.EQ,"STAFF")).
				add(new Expression(roleRef.getPool(),"STAFF",Operator.EQ,true))).execute();





		ModelReflector<UserRole> userRoleModelReflector = ModelReflector.instance(UserRole.class);
		List<UserRole> staffUsers = new Select().from(UserRole.class).where(
				new Expression(userRoleModelReflector.getPool(),"ROLE_ID",Operator.IN, DataSecurityFilter.getIds(staffRoles).toArray())
		).execute();

		Set<Long> userIds = staffUsers.stream().map(su->su.getUserId()).collect(Collectors.toSet());


		Company company = getProxy();

		ModelReflector<User> ref = ModelReflector.instance(User.class);
		Expression where = new Expression(ref.getPool(),Conjunction.AND);
		where.add(new Expression(ref.getPool(), "COMPANY_ID", Operator.EQ, company.getId()));
		where.add(new Expression(ref.getPool(),"USER_ID",Operator.IN,userIds.toArray()));


		Select select = new Select("ID").from(User.class).where(where);

		/*
		select.add(" and exists (select 1 from user_roles , roles where user_roles.user_id = users.id " +
						" and  roles.id = user_roles.role_id " +
						" and ( roles.name = 'STAFF' or roles.staff = true ) )" );
		*/
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
