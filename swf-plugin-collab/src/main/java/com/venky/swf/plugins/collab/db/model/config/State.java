package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
@CONFIGURATION
@ORDER_BY("NAME")
@EXPORTABLE(false)
public interface State extends Model{
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);

	@UNIQUE_KEY("K1,K2")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.ONE)
	public Long getCountryId();
	public void setCountryId(Long iCountryId);
	public Country getCountry();
	
	public List<City> getCities(); 
	
	public static State findByCountryAndName(String  countryName , String stateName) { 
		return findByCountryAndName(Country.findByName(countryName).getId(), stateName);
	}
	public static State findByCountryAndName(Long countryId , String stateName) {
		return findByCountryAndName(countryId,stateName,true);
	}
	public static State findByCountryAndName(Long countryId , String stateName,boolean createIfAbsent) {
		if (stateName == null || countryId == null){
			return null;
		}
		Select s = new Select().from(State.class);
		Expression where = new Expression(s.getPool(), Conjunction.AND);
		where.add(new Expression(s.getPool(),"COUNTRY_ID",Operator.EQ,countryId));

		Expression nameWhere = new Expression(s.getPool(),Conjunction.OR);
		nameWhere.add(new Expression(s.getPool(),"lower(NAME)",Operator.EQ,stateName.toLowerCase()));
		nameWhere.add(new Expression(s.getPool(),"CODE",Operator.EQ,stateName));


		where.add(nameWhere);
		List<State> states = s.where(where).execute(); 
		if (states.size() == 1) {
			return states.get(0);
		}else if (createIfAbsent){
			State state = Database.getTable(State.class).newRecord(); 
			state.setCountryId(countryId);
			state.setName(stateName);
			state.save();
			return state;
		}else {
			return null;
		}
	}
	public static State findByCountryAndCode(Long countryId , String stateCode)  {
		return findByCountryAndCode(countryId,stateCode,true);
	}
	public static State findByCountryAndCode(Long countryId , String stateCode, boolean createIfAbsent) {
		Select s = new Select().from(State.class);
		Expression where = new Expression(s.getPool(), Conjunction.AND);
		where.add(new Expression(s.getPool(),"CODE",Operator.EQ,stateCode));
		where.add(new Expression(s.getPool(),"COUNTRY_ID",Operator.EQ,countryId));

		List<State> states = s.where(where).execute();
		if (states.size() == 1) {
			return states.get(0);
		}else if ( createIfAbsent ){
			State state = Database.getTable(State.class).newRecord();
			state.setCountryId(countryId);
			state.setName(stateCode);
			state.setCode(stateCode);
			state.save();
			return state;
		}else {
			return null;
		}
	}

	@IS_NULLABLE
	@COLUMN_SIZE(2)
	@UNIQUE_KEY("K2")
    public String getCode();
	public void setCode(String code);
}
