package com.venky.swf.util;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.JSON;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.Stack;

public class OidProvider {
    public static Map<String, Map<String,String>>   getHumBolProviders() {
        Map<String, Map<String,String>> groupMap = new Cache<String, Map<String, String>>() {
            @Override
            protected Map<String, String> getValue(String groupKey) {
                return new Cache<String, String>() {
                    @Override
                    protected String getValue(String key) {
                        return Config.instance().getProperty(String.format("swf.%s.%s",groupKey,key));
                    }
                };
            }
        };
        for (String humBolKeys : Config.instance().getPropertyKeys("swf\\.HUMBOL.*\\..*")){
            String[] group = humBolKeys.split("\\.");
            String groupKey = group[1];
            StringBuilder key = new StringBuilder();
            for (int i = 2; i < group.length ; i ++ ){
                if (i > 2){
                    key.append(".");
                }
                key.append(group[i]);
            }
            groupMap.get(groupKey).get(key.toString());
        }
        return groupMap;
        
    }
    
    public static  User initializeUser(JSONObject userObject, String apiKey) {
        cleanUpId(userObject);
        String name = (String)userObject.get("Name");
        User u = Database.getTable(User.class).newRecord();
        u.setName(name);
        u = Database.getTable(User.class).getRefreshed(u);
        
        if (apiKey != null) {
            u.setApiKey(apiKey); //Api key may have changed!! Read by name and update api key.
        }
        u.save(); //Very important so that all child objects can be created as this user.
        User currentUser = Database.getInstance().getCurrentUser();
        
        
        if (currentUser == null) {
            Database.getInstance().open(u); //needed to propagate u.Id to other objects.
        }
        
        u = ModelIOFactory.getReader(User.class, JSONObject.class).read(userObject,true); //Save recursive objs as loggedin  user.
        u.save(); //Other Attributes of user may change. Save it.
        if (currentUser == null) {
            Database.getInstance().open(null);
        }
        return u;
    }
    private static void cleanUpId(JSONObject userInfo){
        Stack<JSONObject> s = new Stack<>();
        s.push(userInfo);
        while (!s.isEmpty()){
            JSON e = new JSON(s.pop());
            e.removeAttribute("Id");
            for (String name : e.getElementAttributeNames()){
                s.push(e.getElementAttribute(name));
            }
            for (String name : e.getArrayElementNames()){
                for (JSONObject o : e.getArrayElements(name)){
                    s.push(o);
                }
            }
        }
        Registry.instance().callExtensions("oid.user.pre.init",userInfo);
    }
}
