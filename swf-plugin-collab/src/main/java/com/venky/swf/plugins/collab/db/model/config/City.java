package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

@CONFIGURATION
public interface City extends Model {
	@Index
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	@Index
	@UNIQUE_KEY
	@IS_NULLABLE(false)
	public Integer getStateId();
	public void setStateId(Integer id);
	public State getState();
	
	public static City findByCountryAndStateAndName(String  countryName , String stateName,String cityName) { 
		return findByStateAndName(State.findByCountryAndName(countryName, stateName).getId(), cityName);
	}
	public static City findByStateAndName(int stateId, String cityName) {
		Select s = new Select().from(City.class); 
		Expression where = new Expression(s.getPool(),Conjunction.AND);
		where.add(new Expression(s.getPool(),"NAME",Operator.EQ,cityName));
		where.add(new Expression(s.getPool(),"STATE_ID",Operator.EQ,stateId));
		
		List<City> cities = s.where(where).execute(); 
		if (cities.size() == 1) {
			return cities.get(0);
		}else {
			City city = Database.getTable(City.class).newRecord(); 
			city.setStateId(stateId);
			city.setName(cityName);
			city.save();
			return city;
		}
	}
}
