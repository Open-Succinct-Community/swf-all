package com.venky.swf.plugins.lucene.configuration;

import java.io.IOException;
import java.util.List;

import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class AppInstaller implements Installer{
	
	public void install(){
		
		Table<IndexDirectory> table = Database.getTable(IndexDirectory.class);
		for (String tableName: Database.getTableNames()){
			
			if (tableName.equals(table.getTableName())){
				continue;
			}
			
			Table<? extends Model> currentTable = Database.getTable(tableName);
			if (!tableName.equals(currentTable.getRealTableName())){
				continue;
			}
			
			//Do this only for real Tables.
			LuceneIndexer indexer = LuceneIndexer.instance(currentTable.getReflector());
			if (indexer.hasIndexedFields()){
				mkdir(currentTable);
			}
		}
		mkdir("MODEL");
	}
	private boolean mkdir(String tableName){
		boolean created = false;
		ModelReflector<IndexDirectory> ref = ModelReflector.instance(IndexDirectory.class);
		List<IndexDirectory> dirs = new Select().from(IndexDirectory.class).where(new Expression(ref.getPool(),ref.getColumnDescriptor("NAME").getName(),Operator.EQ,tableName)).execute();
		if (dirs.isEmpty()){
			IndexDirectory rec = Database.getTable(IndexDirectory.class).newRecord();
			rec.setName(tableName);
			rec.save();
			created = true;
		}
		return created;
	}
	private <M extends Model> void mkdir(Table<M> currentTable){
		if (mkdir(currentTable.getTableName())){
			List<Model> records = new Select().from(currentTable.getModelClass()).execute();
			for (Model m:records){
				try {
					LuceneIndexer.instance(currentTable.getModelClass()).addDocument(m.getRawRecord());
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}
}
