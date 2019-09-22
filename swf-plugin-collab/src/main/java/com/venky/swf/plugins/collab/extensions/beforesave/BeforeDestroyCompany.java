package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelCreateExtension;
import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

public class BeforeDestroyCompany extends BeforeModelDestroyExtension<Company> {
    static {
        registerExtension(new BeforeDestroyCompany());
    }
    @Override
    public void beforeDestroy(Company model) {
        User currentUser = Database.getInstance().getCurrentUser().getRawRecord().getAsProxy(User.class);

        if (currentUser != null && ObjectUtil.equals(currentUser.getCompanyId() , model.getId())){
            throw new RuntimeException("Cannot delete your own company!");
        }
    }
}
