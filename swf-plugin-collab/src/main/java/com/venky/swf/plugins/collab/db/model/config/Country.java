package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

@CONFIGURATION
public interface Country extends Model{
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);
	
	@UNIQUE_KEY("ISO")
	@Index
	public String getIsoCode();
	public void setIsoCode(String isoCode);
	public List<State> getStates(); 
	
	public static Country findByName(String name) { 
		Select s = new Select().from(Country.class); 
		s.where(new Expression(s.getPool(),"NAME",Operator.EQ,name));
		List<Country> country = s.execute(); 
		if (country.size() == 1) {
			return country.get(0);
		}else {
			throw new RuntimeException("Country " + name + " not found!");
		}
	}
	public static Country findByISO(String isoCode) { 
		Select s = new Select().from(Country.class); 
		s.where(new Expression(s.getPool(),"ISO_CODE",Operator.EQ,isoCode));
		List<Country> country = s.execute(); 
		if (country.size() == 1) {
			return country.get(0);
		}else {
			throw new RuntimeException("Country " + isoCode + " not found!");
		}
	}
	
}
