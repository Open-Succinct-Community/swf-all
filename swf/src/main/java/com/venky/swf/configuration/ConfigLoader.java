package com.venky.swf.configuration;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Count;
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
			if (currentTable.getReflector() == null){
				continue;
			}
			if (currentTable.getReflector().isAnnotationPresent(CONFIGURATION.class)) {
				List<Count> counts = new Select("COUNT(1) AS COUNT").from(currentTable.getModelClass()).execute(Count.class);
				Count count = counts.get(0);
				if (count.getCount() < Config.instance().getLongProperty("swf.load.complete.config.tables.if.count.less.than", 500L)){
					new Select().from(currentTable.getModelClass()).execute(); // Loading Complete
				}
			}
		}
		
	}

}
