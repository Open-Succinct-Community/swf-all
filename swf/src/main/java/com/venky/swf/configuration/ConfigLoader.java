package com.venky.swf.configuration;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Counts;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Select;

public class ConfigLoader implements Installer {

	public void install() {
		for (String tableName : Database.getTableNames()){
			Table<? extends Model> currentTable = Database.getTable(tableName);
			if (!tableName.equals(currentTable.getRealTableName())){
				continue;
			}
			if (currentTable.getReflector().isAnnotationPresent(CONFIGURATION.class)) {
				List<Counts> counts = new Select("COUNT(1) AS COUNT").from(currentTable.getModelClass()).execute(Counts.class);
				Counts count = counts.get(0);
				if (count.getCount() < Config.instance().getIntProperty("swf.load.complete.config.tables.if.count.less.than", 500)){
					new Select().from(currentTable.getModelClass()).execute(); // Loading Complete
				}
			}
		}
		
	}

}
