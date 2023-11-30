package com.venky.swf.plugins.lucene.index.common;

import com.venky.cache.Cache;
import com.venky.swf.db.table.Record;

import java.io.Serializable;

public class IndexUpdateTracker implements Serializable{
    public static class IndexUpdateMeta implements Serializable {
        IndexOperation operation;
        Record finalState;

        public IndexOperation getOperation() {
            return operation;
        }

        public Record getFinalState() {
            return finalState;
        }


    }

    public enum IndexOperation implements Serializable{
        added,
        updated,
        removed
    }

    final Cache<String,Cache<Long,IndexUpdateMeta>> recordState = new Cache<>(0,0) {
        @Override
        protected Cache<Long, IndexUpdateMeta> getValue(String tableName) {
            return new Cache<>(0,0) {
                @Override
                protected IndexUpdateMeta getValue(Long Id) {
                    return null;
                }
            };
        }
    };
    public Cache<String,Cache<Long,IndexUpdateMeta>> getRecordState(){
        return recordState;
    }
    public void update(String table,Record record,IndexOperation operation){
        if (record.get("ID") == null){
            return;
        }
        Long id = record.getId();

        Cache<Long,IndexUpdateMeta> indexUpdateMetaCache = recordState.get(table);
        IndexUpdateMeta meta  = indexUpdateMetaCache.get(id);
        if (meta == null){
            meta = new IndexUpdateMeta();
            meta.finalState = record;
            meta.operation = operation;
            indexUpdateMetaCache.put(id,meta);
            if (meta.finalState.getFieldNames().isEmpty() || record.getFieldNames().isEmpty()){
                throw new RuntimeException("Got Blank");
            }

        }else {
            if (meta.finalState.getFieldNames().isEmpty() || record.getFieldNames().isEmpty()){
                throw new RuntimeException("Got Blank");
            }
            meta.finalState.merge(record);
            if (operation == IndexOperation.removed ){
                if (meta.operation == IndexOperation.added) {
                    indexUpdateMetaCache.remove(id);
                }else if (meta.operation == IndexOperation.updated) {
                    meta.operation = IndexOperation.removed;
                }
            }
            if (meta.finalState.getFieldNames().isEmpty() || record.getFieldNames().isEmpty()){
                throw new RuntimeException("Got Blank");
            }
        }

    }

}
