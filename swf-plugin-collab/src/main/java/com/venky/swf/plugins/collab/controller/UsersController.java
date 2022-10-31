package com.venky.swf.plugins.collab.controller;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.views.View;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UsersController extends com.venky.swf.controller.UsersController {
    public UsersController(Path path) {
        super(path);
    }

    public View addPhone(String phoneNumber){
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        User user = users.get(0).getRawRecord().getAsProxy(User.class);
        if (user.getRawRecord().isNewRecord()){
            throw new RuntimeException("User not registered yet.");
        }

        String sanitizePhoneNumber  = Phone.sanitizePhoneNumber(phoneNumber);
        List<UserPhone> userPhones = user.getUserPhones().stream().filter(up->ObjectUtil.equals(up.getPhoneNumber(),sanitizePhoneNumber)).collect(Collectors.toList());
        if (userPhones.isEmpty()){
            UserPhone userPhone = Database.getTable(UserPhone.class).newRecord();
            userPhone.setPhoneNumber(phoneNumber);
            userPhone.setUserId(user.getId());
            userPhone.save();
        }

        return getIntegrationAdaptor().createResponse(getPath(),user, getIncludedModelFields().get(User.class), new HashSet<>(),getIncludedModelFields());
    }
    public View addEmail(String emailId){
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        User user = users.get(0).getRawRecord().getAsProxy(User.class);
        if (user.getRawRecord().isNewRecord()){
            throw new RuntimeException("User not registered yet.");
        }
        List<UserEmail> userEmails = user.getUserEmails().stream().filter(up->ObjectUtil.equals(up.getEmail(),emailId)).map(ue->ue.getRawRecord().getAsProxy(UserEmail.class)).collect(Collectors.toList());
        if (userEmails.isEmpty()){
            UserEmail userEmail = Database.getTable(UserEmail.class).newRecord();
            userEmail.setEmail(emailId);
            userEmail.setUserId(user.getId());
            userEmail.save();
        }

        return getIntegrationAdaptor().createResponse(getPath(),user, getIncludedModelFields().get(User.class), new HashSet<>(),getIncludedModelFields());
    }

    @Override
    protected String[] getIncludedFields() {
        Map<Class<? extends Model>, List<String>> map  = getIncludedModelFields();
        if (map.containsKey(User.class)){
            return map.get(User.class).toArray(new String[]{});
        }else {
            return null;
        }
    }

    @Override
    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>,List<String>> map = super.getIncludedModelFields();
        if( getReturnIntegrationAdaptor() == null ){
            return map ;
        }
        if (!map.containsKey(User.class)) {
            map.put(User.class, Arrays.asList("ID", "NAME"));
        }
        if (!map.containsKey(UserPhone.class)) {
            map.put(UserPhone.class, Arrays.asList("ID", "PHONE_NUMBER", "VALIDATED"));
        }
        if (!map.containsKey(UserEmail.class)){
            map.put(UserEmail.class, Arrays.asList("ID", "EMAIL", "VALIDATED"));
        }
        return map;
    }

}
