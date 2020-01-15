/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserEmail;

/**
 *
 * @author venky
 */
public class AfterSaveUser extends AfterModelSaveExtension<User>{
    static {
        registerExtension(new AfterSaveUser());
    }
    @Override
    public void afterSave(final User model) {
        model.getUserEmails().forEach((UserEmail ue)->{
            if (!ObjectUtil.equals(ue.getAlias(),model.getLongName())){
                ue.setAlias(model.getLongName());
                ue.save();
            }
        });
    }
    
}
