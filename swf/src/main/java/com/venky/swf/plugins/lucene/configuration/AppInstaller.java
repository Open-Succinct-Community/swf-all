package com.venky.swf.plugins.lucene.configuration;

import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.extensions.LuceneBeforeCommitExtension.TableRecordSetIndexer;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class AppInstaller implements Installer{
	
	public void install(){
		
		List<Task> tasks = new ArrayList<>();
		for (String tableName: Database.getTableNames()){
			

			Table<? extends Model> currentTable = Database.getTable(tableName);
            if (currentTable == null){
                return;
            }
			if (!tableName.equals(currentTable.getRealTableName())){
				continue;
			}
			
			//Do this only for real Tables.
			if (currentTable.getReflector() == null){
				continue;
			}
			if (currentTable.getReflector().isVirtual()){
			    continue;
            }
			LuceneIndexer indexer = LuceneIndexer.instance(currentTable.getReflector());
			if (indexer.hasIndexedFields()){
				tasks.add(new Mkdir(currentTable.getTableName()));
			}
		}
		mkdir("MODEL");
        TaskManager.instance().executeAsync(tasks,false);
	}
	public static class Mkdir implements Task {
        String tableName;
 	    public Mkdir(String tableName){
            this.tableName = tableName;
        }
        public Mkdir(){

        }
        @Override
        public void execute() {
            mkdir(Objects.requireNonNull(Database.getTable(this.tableName)));
        }

	}
    private static <M extends Model> void mkdir(Table<M> currentTable){
        if (mkdir(currentTable.getTableName())){
            TaskManager.instance().execute(new TableRecordSetIndexer(currentTable.getTableName(),new ArrayList<>()));
            TaskManager.instance().executeAsync(new TableIndexer<>(currentTable),false);
        }
    }
    public static class TableIndexer<M extends Model> implements Task {
        long startId;
        Table<M> currentTable;
        public TableIndexer(Table<M> table){
            this(table,0);
        }
        public TableIndexer(Table<M> table,long startId){
            this.currentTable = table;
            this.startId = startId;
        }

        @Override
        public void execute() {
            int BATCH = 5000;
            List<M> records = new Select().from(currentTable.getModelClass()).where(new Expression(currentTable.getPool(),"ID",Operator.GT,startId)).orderBy("ID").execute(BATCH);

            for (M m:records){
                try {
                    LuceneIndexer.instance(currentTable.getModelClass()).addDocument(m.getRawRecord());
                } catch (IOException e) {
                    Config.instance().getLogger(getClass().getName()).log(Level.WARNING,String.format("Indexing failed for id = %d" , m.getId()),e);
                    break;
                }
            }
            if (records.size() >= BATCH){
                M last = records.get(records.size()-1);
                TaskManager.instance().executeAsync(new TableIndexer<>(currentTable,last.getId()),false);
            }
        }
    }
    private static boolean mkdir(String tableName){
        boolean created = false;
        File baseDir = new File(Config.instance().getProperty("swf.index.dir",".index"));
        File dir = new File(baseDir,tableName);
        if (!dir.exists()){
            created = dir.mkdirs();
        }
        return created;
    }

}
