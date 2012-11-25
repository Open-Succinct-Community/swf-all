package com.venky.swf.db.model.io.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.AbstractModelReader;

public class JSONModelReader<M extends Model> extends AbstractModelReader<M, JSONObject> {

	public JSONModelReader(Class<M> beanClass) {
		super(beanClass);
	}
	
	@Override
	public List<M> read(InputStream in) throws IOException {
		try {
			JSONParser parser = new JSONParser();
			List<M> list = new ArrayList<M>();
			JSONObject jsIn = (JSONObject)parser.parse(new InputStreamReader(in));

			String attrName = StringUtil.pluralize(getBeanClass().getSimpleName());
			JSONArray jsArr = (JSONArray)jsIn.get(attrName);
			if (jsArr != null){
				for (Object obj : jsArr){
					JSONObject json = (JSONObject)obj;
					JSONObject element = (JSONObject) json.get(getBeanClass().getSimpleName());
					list.add(read(element));
				}
			}else {
				list.add(read(jsIn));
			}
			
			return list;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	

}
