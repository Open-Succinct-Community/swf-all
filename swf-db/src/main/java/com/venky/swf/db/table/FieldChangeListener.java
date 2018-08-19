package com.venky.swf.db.table;

import com.venky.core.util.ChangeListener;

public class FieldChangeListener implements ChangeListener {
    public FieldChangeListener(){

    }
    public FieldChangeListener(Record record , String fieldName){
        this.record = record;
        this.fieldName = fieldName;
    }
    Record record;
    String fieldName;
    @Override
    public void hasChanged(Object oldValue, Object newValue) {
        record.markDirty(fieldName,oldValue,newValue);
    }
}
