package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

@MENU("World")
public interface WorldCurrency extends Model {

    @Index
    @UNIQUE_KEY("NAME")
    public String getName();
    public void setName(String name);

    @Index
    @UNIQUE_KEY("CODE")
    public String getCode();
    public void setCode(String code);

    @Index
    public String getSymbol();
    public void setSymbol(String symbol);

    static WorldCurrency find(String codeOrName){
        Select select = new Select().from(WorldCurrency.class);

        select.where(new Expression(select.getPool(), Conjunction.OR).
                add(new Expression(select.getPool(),"CODE", Operator.EQ,codeOrName)).
                add(new Expression(select.getPool(),"NAME", Operator.EQ,codeOrName)));
        List<WorldCurrency> currencyList = select.execute();
        if (currencyList.isEmpty()){
            throw new RuntimeException("Unknown currency " + codeOrName);
        }else {
            return currencyList.get(0);
        }
    }
}
