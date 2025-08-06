package com.venky.swf.plugins.sequence.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.sequence.db.model.SequentialNumber;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.Arrays;
import java.util.List;

public class SequentialNumbersController extends ModelController<SequentialNumber> {

    public SequentialNumbersController(Path path) {
        super(path);
    }

    @RequireLogin(value = false)
    public View next(String sequenceName){
        SequentialNumber number = SequentialNumber.get(sequenceName);
        number.increment();
        if (getIntegrationAdaptor() != null){
            return getIntegrationAdaptor().createResponse(getPath(), number, List.of("CURRENT_NUMBER"));
        }else {
            return  new BytesView(getPath(),number.getCurrentNumber().getBytes());
        }
    }
}
