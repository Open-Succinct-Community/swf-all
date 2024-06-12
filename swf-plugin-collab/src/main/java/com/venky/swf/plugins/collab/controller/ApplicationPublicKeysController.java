package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey;
import com.venky.swf.views.View;

public class ApplicationPublicKeysController extends ModelController<ApplicationPublicKey> {
    public ApplicationPublicKeysController(Path path) {
        super(path);
    }

    @SingleRecordAction(icon = "fa-check")
    public View verify(long id ){
        ApplicationPublicKey key = Database.getTable(getModelClass()).get(id);
        key.setTxnProperty("being.verified", true);
        key.setVerified(true);
        key.save();
        return show(key);
    }
}
