package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.extensions.BeforeModelCreateExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

public class BeforeCreateCompany extends BeforeModelCreateExtension<Company> {
    static {
        registerExtension(new BeforeCreateCompany());
    }
    @Override
    public void beforeCreate(Company model) {
        if (model.getCreatorUserId() != null){
            Long creatorCompanyId = model.getCreatorUser().getRawRecord().getAsProxy(User.class).getCompanyId();
            if (creatorCompanyId != null){
                model.setCreatorCompanyId(creatorCompanyId);
            }
        }

    }
}
