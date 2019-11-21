package com.venky.swf.plugins.sequence.db.model;

import com.venky.swf.db.table.ModelImpl;

public class SequentialNumberImpl extends ModelImpl<SequentialNumber> {
    public SequentialNumberImpl(SequentialNumber proxy){
        super(proxy);
    }
    public SequentialNumberImpl(){

    }

    public String next(){
        increment();
        return getProxy().getCurrentNumber();
    }
    public void increment(){
        SequentialNumber number = getProxy();
        String value  =  number.getCurrentNumber();
        String nextNumber = String.format("%0"+value.length()+"d", Long.valueOf(value)+1L);
        number.setCurrentNumber(nextNumber);
        number.save();
    }
}
