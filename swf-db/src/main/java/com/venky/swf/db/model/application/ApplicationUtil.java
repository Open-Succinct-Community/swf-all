package com.venky.swf.db.model.application;

import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class ApplicationUtil {
    public static Application find(String appId){
        ModelReflector<Application> ref = ModelReflector.instance(Application.class);
        List<Application> applications = new Select().from(Application.class).where(new Expression(ref.getPool(),"APP_ID", Operator.EQ, appId)).execute();
        if (applications.size() != 1){
            return null;
        }
        return applications.get(0);
    }
}
