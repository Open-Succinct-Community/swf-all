package com.venky.swf.plugins.sequence.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

@MENU("Configuration")
public interface SequentialNumber extends Model {

    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    @COLUMN_DEF(value = StandardDefault.SOME_VALUE, args = "0000000000")
    public String getCurrentNumber();
    public void setCurrentNumber(String number);

    public String next();
    public void increment();

    public static SequentialNumber get(String sequenceName){
        Select select = new Select(true).from(SequentialNumber.class);
        Expression where = new Expression(select.getPool(),"NAME", Operator.EQ,sequenceName);
        List<SequentialNumber> list = select.where(where).execute();
        if (list.size() == 1){
            return list.get(0);
        }else if (list.isEmpty()) {
            SequentialNumber number = Database.getTable(SequentialNumber.class).newRecord();
            number.setName(sequenceName);
            number.save();
            return  number;
        }else{
            throw new IllegalArgumentException("No Unique sequence found by name : " + sequenceName);
        }

    }

}
