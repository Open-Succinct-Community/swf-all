package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.openhtmltopdf.css.parser.property.CounterPropertyBuilder.CounterReset;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.path._IPath;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

@CONFIGURATION
@EXPORTABLE(false)
@MENU("World")
public interface Country extends Model{
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);

	@COLUMN_NAME("ISO_CODE")
	@IS_NULLABLE
	@UNIQUE_KEY("CODE")
	public String getCode();
	public void setCode(String code);

	@UNIQUE_KEY("ISO")
	@Index
	public String getIsoCode();
	public void setIsoCode(String isoCode);

	@UNIQUE_KEY("ISO2")
	@Index
	public String getIsoCode2();
	public void setIsoCode2(String isoCode);


	public List<State> getStates(); 
	
	public static Country findByName(String name) { 
		Select s = new Select().from(Country.class);
		s.where(new Expression(s.getPool(),"lower(NAME)",Operator.EQ,name.toLowerCase()));
		List<Country> countries = s.execute();
		Country country = null;
		if (countries.size() == 1) {
			country = countries.get(0);
		}
		if (country == null){
			country = findByISO(name);
		}
        return country;
	}
	public static Country findByISO(String isoCode) {
		Select s = new Select().from(Country.class); 
		s.where(new Expression(s.getPool(), Conjunction.OR).
				add(new Expression(s.getPool(),"ISO_CODE",Operator.EQ,isoCode)).add(new Expression(s.getPool(),"ISO_CODE_2",Operator.EQ,isoCode)));
		List<Country> country = s.execute(); 
		if (country.size() == 1) {
			return country.get(0);
		}else {
			throw new RuntimeException("Country " + isoCode + " not found!");
		}
	}
	
}
