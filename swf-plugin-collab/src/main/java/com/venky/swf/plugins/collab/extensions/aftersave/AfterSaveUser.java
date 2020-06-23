package com.venky.swf.plugins.collab.extensions.aftersave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AfterSaveUser extends AfterModelSaveExtension<User> {
    static {
        registerExtension(new AfterSaveUser());
    }

    @Override
    public void afterSave(User user) {
        if (!ObjectUtil.isVoid(user.getEmail())){
            List<UserEmail> emails = new ArrayList<>();
            user.getUserEmails().forEach(ue->{
                if (ObjectUtil.equals(ue.getEmail(), user.getEmail())){
                    emails.add(ue.getRawRecord().getAsProxy(UserEmail.class));
                }
            });

            if (emails.isEmpty()) {
                UserEmail email = Database.getTable(UserEmail.class).newRecord();
                email.setUserId(user.getId());
                email.setEmail(user.getEmail());
                email.setAlias(user.getLongName() == null ? user.getEmail() : user.getLongName());
                email.save();
            }
        }
        if (!ObjectUtil.isVoid(user.getPhoneNumber())){
            List<UserPhone> phones = user.getUserPhones().stream().filter(userPhone -> ObjectUtil.equals(user.getPhoneNumber(), userPhone.getPhoneNumber())).
                    collect(Collectors.toList());

            if (phones.isEmpty()) {
                UserPhone phone = Database.getTable(UserPhone.class).newRecord();
                phone.setUserId(user.getId());
                phone.setPhoneNumber(user.getPhoneNumber());
                phone.save();
            }
        }

    }
}
