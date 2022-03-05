package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelCreateExtension;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public class AfterSaveCompany extends AfterModelSaveExtension<Company> {
    static {
        registerExtension(new AfterSaveCompany());
    }
    @Override
    public void afterSave(Company model) {
        if (model.getReflector().isVoid(model.getCreatorCompanyId())){
            TaskManager.instance().executeAsync((Task) () -> {
                Company c = Database.getTable(Company.class).get(model.getId());
                c.setCreatorCompanyId(model.getId());
                c.save();
            },false);
        }
    }
}
