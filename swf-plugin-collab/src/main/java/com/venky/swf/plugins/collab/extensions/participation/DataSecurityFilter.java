package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model._Identifiable;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class DataSecurityFilter {
	public static List<Company> getCompaniesAccessible(User by){
		Select s = new Select().from(Company.class).where(by.getDataSecurityWhereClause(Company.class));
		List<Company> companies = s.execute();
		return companies;
	}
	public static List<Facility> getFacilitiesAccessible(User by){
		Select s = new Select().from(Facility.class).where(by.getDataSecurityWhereClause(Facility.class));
		List<Facility> facilities = s.execute();
		return facilities;
	}
	public static List<User> getUsersAccessible(User by){
		Select s = new Select().from(User.class).where(by.getDataSecurityWhereClause(User.class));
		List<User> users = s.execute();
		return users;
	}
	
	public static List<User> getAllowedUsers(Company company){
		Expression where = new Expression(Conjunction.OR);
		where.add(new Expression("COMPANY_ID",Operator.EQ,company.getId()));
		where.add(new Expression("ID",Operator.EQ,company.getCreatorUserId()));
		
		Select sel = new Select().from(User.class).where(where);
		return sel.execute();
	}
	
	public static List<User> getAllowedUsers(Facility facility){
		return getAllowedUsers(facility.getCompany());
	}

	public static SequenceSet<Integer> getIds(List<? extends _Identifiable> idables){
		SequenceSet<Integer> ret = new SequenceSet<Integer>();
		for (_Identifiable idable: idables){
			ret.add(idable.getId());
		}
		return ret;
	}
}
