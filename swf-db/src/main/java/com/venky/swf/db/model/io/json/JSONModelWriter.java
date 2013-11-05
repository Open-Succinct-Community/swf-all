package com.venky.swf.db.model.io.json;

import org.json.simple.JSONObject;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.AbstractModelWriter;

public class JSONModelWriter<M extends Model> extends AbstractModelWriter<M, JSONObject>{

	public JSONModelWriter(Class<M> beanClass) {
		super(beanClass);
	}
}
