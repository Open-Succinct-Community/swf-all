package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;

public class RequestAuthenticator implements Extension {
    static{
        Registry.instance().registerExtension(Path.REQUEST_AUTHENTICATOR,new RequestAuthenticator());
    }
    @Override
    public void invoke(Object... context) {
        Path path = (Path)context[0];
        ObjectHolder<User> userObjectHolder = (ObjectHolder<User>)context[1];

        String apiKey = path.getRequest().getHeader("X-ApiKey");

        if (ObjectUtil.isVoid(apiKey)){
            apiKey = path.getRequest().getHeader("ApiKey");
        }

        if (ObjectUtil.isVoid(apiKey)){
            apiKey = path.getRequest().getParameter("ApiKey");
        }


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
        }
    }
}
