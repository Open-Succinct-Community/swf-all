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
		return read(in,getBeanClass().getSimpleName());
	}
	
	@Override
	public List<M> read(InputStream in,String rootElementName) throws IOException{
		try {
			JSONParser parser = new JSONParser();
			List<M> list = new ArrayList<M>();
			
			JSONObject jsIn = (JSONObject)parser.parse(new InputStreamReader(in));
			JSONObject singularObject = (JSONObject)jsIn.get(rootElementName);
			if (singularObject != null){
				list.add(read(singularObject));
			}else {
				String attrName = StringUtil.pluralize(rootElementName);
				if (jsIn.keySet().size() == 1){
					if (!jsIn.containsKey(attrName)){
						Object o  = jsIn.values().iterator().next();
						if (o instanceof JSONObject){
							jsIn = (JSONObject)o;
						}
					}
				}

				JSONArray jsArr = (JSONArray)jsIn.get(attrName);
				if (jsArr != null){
					for (Object obj : jsArr){
						JSONObject json = (JSONObject)obj;
						JSONObject element = json;
						Object oelement = json.get(rootElementName);
						if (oelement != null && oelement instanceof JSONObject){
							element = (JSONObject)oelement;
						}
						list.add(read(element));
					}
				}
			}return list;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
