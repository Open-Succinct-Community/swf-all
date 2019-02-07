package com.venky.swf.plugins.lucene.index.agents;

import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.SerializationHelper;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.agent.AgentFinishUpTask;
import com.venky.swf.plugins.background.core.agent.AgentSeederTask;
import com.venky.swf.plugins.background.core.agent.AgentSeederTaskBuilder;
import com.venky.swf.plugins.lucene.db.model.IndexQueue;
import com.venky.swf.plugins.lucene.index.background.IndexTask;
import com.venky.swf.sql.Select;


import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndexUpdatorAgent implements AgentSeederTaskBuilder{

    @Override
    public AgentSeederTask createSeederTask() {
        return new IndexUpdatorAgentSeeder() ;
    }
    public static final String INDEX_UPDATOR_AGENT = "INDEX_UPDATOR_AGENT";

    public static class IndexUpdatorAgentSeeder extends AgentSeederTask {

        @Override
        public List<Task> getTasks() {
            int maxRecords = 1000; //Actually commit count;
            Bucket numRecordsProcessed = new Bucket();
            List<Task> tasks = new ArrayList<>();
            SerializationHelper helper = new SerializationHelper();
            new Select(){
                @Override
                protected boolean isCacheable(ModelReflector<? extends Model> ref) {
                    return false;
                }
            }.from(IndexQueue.class).orderBy("CREATED_AT","ID").execute(IndexQueue.class, maxRecords +1 ,new Select.ResultFilter<IndexQueue>() {
                @Override
                public boolean pass(IndexQueue record) {
                    InputStream ois = record.getIndexTask();
                    try {
                        IndexTask task = helper.read(ois);
                        task.execute();
                        record.destroy();
                        numRecordsProcessed.increment();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }finally {
                        if (ois != null) {
                            try {
                                ois.close();
                            } catch (IOException ex) {

                            } finally {
                                ois = null;
                            }
                        }
                    }
                    return false;
                }
            });
            if (numRecordsProcessed.intValue() > 0){
                tasks.add(this);
            }else {
                tasks.add(new AgentFinishUpTask(getAgentName(),canExecuteRemotely()));
            }
            return tasks;
        }

        @Override
        public String getAgentName() {
            return INDEX_UPDATOR_AGENT;
        }


    }
}
