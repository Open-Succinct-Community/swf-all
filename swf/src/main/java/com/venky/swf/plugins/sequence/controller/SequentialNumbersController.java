package com.venky.swf.plugins.sequence.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.sequence.db.model.SequentialNumber;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.Arrays;

public class SequentialNumbersController extends ModelController<SequentialNumber> {

    public SequentialNumbersController(Path path) {
        super(path);
    }

    public View next(String sequenceName){
        SequentialNumber number = SequentialNumber.get(sequenceName);
        number.increment();
        if (getIntegrationAdaptor() != null){
            return getIntegrationAdaptor().createResponse(getPath(), number, Arrays.asList("CURRENT_VALUE"));
        }else {
            return  new BytesView(getPath(),number.getCurrentNumber().getBytes());
        }
    }
}
