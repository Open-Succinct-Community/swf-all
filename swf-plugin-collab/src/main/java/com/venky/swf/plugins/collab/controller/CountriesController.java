package com.venky.swf.plugins.collab.controller;

import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.Country;

public class CountriesController extends OpenSearchController<Country> {
    public CountriesController(Path path) {
        super(path);
    }


}
