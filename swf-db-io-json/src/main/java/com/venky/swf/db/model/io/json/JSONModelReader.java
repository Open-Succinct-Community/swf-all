package com.venky.swf.db.model.io.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.venky.swf.integration.FormatHelper;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
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
	public List<M> read(InputStream in,boolean saveRecursive) throws IOException {
		return read(in, FormatHelper.change_case(getBeanClass().getSimpleName(), KeyCase.CAMEL, Config.instance().getApiKeyCase()),saveRecursive);
	}
	
	@Override
	public List<M> read(InputStream in,String rootElementName,boolean saveRecursive) throws IOException{
		try {
			JSONParser parser = new JSONParser();
			List<M> list = new ArrayList<M>();
			
			JSONAware jsIn = (JSONAware) parser.parse(new InputStreamReader(in));
			JSONArray jsArr = null;
			if ((jsIn instanceof JSONArray)){
				jsArr = (JSONArray) jsIn;
			}else {
				JSONObject possibleSingularObject = (JSONObject) jsIn;
				String attrName = StringUtil.pluralize(rootElementName);
				if (possibleSingularObject.containsKey(attrName)){
					jsArr = (JSONArray)possibleSingularObject.get(attrName);
				}else {
					jsArr = new JSONArray();
					if (possibleSingularObject.containsKey(rootElementName)){
						jsArr.add((JSONObject)possibleSingularObject.get(rootElementName));
					}else {
						jsArr.add(possibleSingularObject);
					}
				}
			}
			for (Object obj : jsArr){
				JSONObject json = (JSONObject)obj;
				JSONObject element = json;
				Object oelement = json.get(rootElementName);
				if (oelement != null && oelement instanceof JSONObject){
					element = (JSONObject)oelement;
				}
				list.add(read(element,saveRecursive));
			}
			return list;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
