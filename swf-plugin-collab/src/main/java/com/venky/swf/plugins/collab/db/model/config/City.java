package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

@CONFIGURATION
@EXPORTABLE(false)
@ORDER_BY("NAME")
@MENU("Geography")
public interface City extends Model , GeoLocation {
	@Index
	@UNIQUE_KEY("K1,K2")
	public String getName();
	public void setName(String name);
	
	@Index
	@UNIQUE_KEY("K1,K2")
	@IS_NULLABLE(false)
	public Long getStateId();
	public void setStateId(Long id);
	public State getState();

	@Index
	@UNIQUE_KEY(value = "K2" , allowMultipleRecordsWithNull = false)
	public String getRegionName();
	public void setRegionName(String region);
	
	public static City findByCountryAndStateAndName(String  countryName , String stateName,String cityName) { 
		return findByStateAndName(State.findByCountryAndName(countryName, stateName).getId(), cityName);
	}
	public static City findByStateAndName(long stateId, String cityName) {
		return findByStateAndName(stateId,cityName,true);
	}
	public static City findByStateAndName(long stateId, String cityName,boolean createIfAbsent) {
		Select s = new Select().from(City.class); 
		Expression where = new Expression(s.getPool(),Conjunction.AND);

		Expression cityClause = new Expression(s.getPool(),Conjunction.OR);
		cityClause.add(new Expression(s.getPool(),"lower(NAME)",Operator.EQ,cityName.toLowerCase()));
		cityClause.add(new Expression(s.getPool(),"CODE",Operator.EQ,cityName.toLowerCase()));

		where.add(cityClause);
		where.add(new Expression(s.getPool(),"STATE_ID",Operator.EQ,stateId));
		
		List<City> cities = s.where(where).execute(); 
		if (cities.size() == 1) {
			return cities.get(0);
		}else if (createIfAbsent) {
			City city = Database.getTable(City.class).newRecord(); 
			city.setStateId(stateId);
			city.setName(cityName);
			city.setCode(cityName);
			city.save();
			return city;
		}else {
			return null;
		}
	}

	@IS_NULLABLE
	@UNIQUE_KEY("K3")
	public String getCode();
	public void setCode(String code);

	public static City findByCode(String code) {
		Select s = new Select().from(City.class);
		Expression where = new Expression(s.getPool(), Conjunction.AND);
		where.add(new Expression(s.getPool(),"CODE", Operator.EQ,code));

		List<City> cities = s.where(where).execute();
		if (!cities.isEmpty()) {
			return cities.get(0);
		}else {
			return null;
		}
	}
}
