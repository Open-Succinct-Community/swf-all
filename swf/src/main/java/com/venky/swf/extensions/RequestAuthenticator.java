package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.User;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;

import java.math.BigDecimal;

public class RequestAuthenticator implements Extension {
    static{
        Registry.instance().registerExtension(Path.REQUEST_AUTHENTICATOR,new RequestAuthenticator());
    }
    @Override
    public void invoke(Object... context) {
        Path path = (Path)context[0];
        ObjectHolder<User> userObjectHolder = (ObjectHolder<User>)context[1];

        String apiKey = getHeader(path,"ApiKey");
        String lat = getHeader( path,"Lat");
        String lng = getHeader( path, "Lng");



        if (!ObjectUtil.isVoid(apiKey)){
            User user = path.getUser("api_key",apiKey);
            if (user == null) {
                path.addErrorMessage("Invalid Api Key");
            }else if (userObjectHolder.get() != null){
                User alreadySet = userObjectHolder.get();
                if (alreadySet.getId() != user.getId()){
                    userObjectHolder.set(null);
                    path.addErrorMessage("User not identifiable");
                }
            }else {
                userObjectHolder.set(user);
            }
            if (user != null){
                TypeConverter<BigDecimal> tc = user.getReflector().getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
                if (!ObjectUtil.isVoid(lat)){
                    user.setCurrentLat(tc.valueOf(lat));
                }
                if (!ObjectUtil.isVoid(lng)){
                    user.setCurrentLng(tc.valueOf(lng));
                }
            }
        }
    }

    public String getHeader(Path path, String key){
        String value = path.getRequest().getHeader("X-"+key);

        if (ObjectUtil.isVoid(value)){
            value = path.getRequest().getHeader(key);
        }

        if (ObjectUtil.isVoid(value)){
            value = path.getRequest().getMethod().equalsIgnoreCase(HttpMethod.GET.toString()) ? path.getRequest().getParameter(key) : null ;
        }
        return value;
    }
}
