package com.venky.swf.plugins.lucene.index.common;

import com.venky.core.collections.SequenceMap;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerUtils;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ModelCollector<M extends Model> implements ResultCollector {
    final int batchSize;
    final int maxRecords;
    final int minDistinctScores;
    final Set<Float> scores = new HashSet<>();
    final Class<M> modelClass ;
    final Expression additionalWhereClause ;
    final Map<Long,M> recordMap = new HashMap<>();
    final Comparator<M> modelComparator ;
    
    public ModelCollector(Class<M> modelClass, int maxRecords){
        this(modelClass, maxRecords,0);
    }
    public ModelCollector(Class<M> modelClass, int maxRecords,int minDistinctScores){
        this(modelClass,maxRecords,minDistinctScores,Math.min(maxRecords,500));
    }
    public ModelCollector(Class<M> modelClass, int maxRecords,int minDistinctScores,int batchSize){
        this(modelClass,maxRecords,minDistinctScores,batchSize,null);
    }
    public ModelCollector(Class<M> modelClass, int maxRecords, int minDistinctScores, int batchSize,Expression additionalWhereClause){
        this(modelClass,maxRecords,minDistinctScores,batchSize,additionalWhereClause,null);
    }
    public ModelCollector(Class<M> modelClass, int maxRecords, int minDistinctScores, int batchSize,Expression additionalWhereClause,Comparator<M> modelComparator){
        this.maxRecords = maxRecords;
        this.batchSize = Math.min(batchSize,maxRecords);
        this.minDistinctScores = minDistinctScores;
        this.modelClass = modelClass;
        this.additionalWhereClause = additionalWhereClause;
        this.modelComparator = Objects.requireNonNullElseGet(modelComparator, () -> (o1, o2) -> (int) (o2.getId() - o1.getId()));
    }
    
    public Class<M> getModelClass(){
        return modelClass;
    }
    public ModelReflector<M> getReflector(){
        return ModelReflector.instance(getModelClass());
    }
    
    public Expression getWhereClause(){
        return additionalWhereClause;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public int getMaxRecords() {
        return maxRecords;
    }
    
    final SequenceMap<Long,ScoreDoc> idsBeingProcessed = new SequenceMap<>();
    
    final SequenceMap<Long,ScoreDoc> allIdsInspected = new SequenceMap<>();
    
    final SWFLogger cat = Config.instance().getLogger(getClass().getName());
    
    @Override
    public void collect(Document d, ScoreDoc scoreDoc) {
        TimerUtils.time(cat,"Collecting Record Ids" , ()->{
            Long id = Long.valueOf(d.getField("ID").stringValue());
            idsBeingProcessed.put(id,scoreDoc);
            return null;
        });
    }
    
    @Override
    public boolean isEnough(){
        if (!idsBeingProcessed.isEmpty()) {
            Select sel = TimerUtils.time(cat, "Forming Select Statement for collected ids" , () ->{
                Select select = new Select();
                select.from(getModelClass());
                select.where(new Expression(getReflector().getPool(), Conjunction.AND){{
                    add(Expression.createExpression(getReflector().getPool(), "ID", Operator.IN, idsBeingProcessed.keySet().toArray()));
                    if (getWhereClause() != null) {
                        add(getWhereClause());
                    }
                }});
                return select;
            });
            
            List<M> tmpList = TimerUtils.time(cat, "Executing Select for collected ids " , ()->{
                return sel.execute(getModelClass());
            });
            
            for (M tmp : tmpList) {
                recordMap.put(tmp.getId(), tmp);
                if (minDistinctScores > 0) {
                    scores.add(idsBeingProcessed.get(tmp.getId()).score);
                }
            }
            allIdsInspected.putAll(idsBeingProcessed);
            idsBeingProcessed.clear();
        }
        int totalNumRecords = recordMap.size();
        int totalScoreCount = scores.size();
        
        return (totalNumRecords >= maxRecords && totalScoreCount >= minDistinctScores);
    }
    
    
    
    public List<M> getRecords(){
        List<M> records = new ArrayList<>();
        int numRecordsAdded = 0;
        for (long id : allIdsInspected.keySet()){
            M record = recordMap.remove(id); //Retains the order from lucene based on scores.
            if (record != null){
                record.setTxnProperty("scoreDoc",allIdsInspected.get(id));
                addRecord(records,record);
                numRecordsAdded ++ ;
            }
        }
        records.sort(modelComparator);
        if (numRecordsAdded >= maxRecords){
            return records.subList(0,maxRecords);
        }else {
            return records;
        }
    }
    protected void addRecord(List<M> records, M record){
        records.add(record);
    }
    
    
}
