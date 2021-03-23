package com.venky.swf.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.io.json.JSONFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.venky.core.string.StringUtil;


public class JSON extends FormatHelper<JSONObject>{

	private JSONObject root = null;
	public JSON(InputStream in) {
		try {
			JSONObject input = (JSONObject)JSONValue.parseWithException(new InputStreamReader(in));
			this.root = input;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public JSON(String name,boolean isPlural){
		this.root = new JSONObject();
		if (isPlural){
			root.put(name, new JSONArray());
		}else {
			root.put(name, new JSONObject());
		}
	}
	
	public JSON(JSONObject obj){
		this.root = obj;
	}
	
	
	public JSONObject getRoot(){
		return root;
	}

	@Override
	public String getRootName() {
		if( root.size() == 1 ){
			return (String)root.keySet().iterator().next();
		}else {
			return null;
		}
	}

	@Override
	public JSONObject changeRootName(String toName) {
		String rootName = getRootName();
		if (rootName != null && !ObjectUtil.equals(rootName,toName)){
			Object v = root.remove(rootName);
			root.put(toName,v);
		}
		return root;
	}


	@SuppressWarnings("unchecked")
	@Override
	public JSONObject createArrayElement(String name){
		String pluralName = StringUtil.pluralize(name);
		JSONArray children = (JSONArray)root.get(pluralName);
		if (children == null){
			children = new JSONArray();
			root.put(pluralName, children);
		}
		
		JSONObject child = new JSONObject();
		children.add(child);
		return child;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<JSONObject> getArrayElements(String name){
		String pluralName = StringUtil.pluralize(name);
		JSONArray children = (JSONArray)root.get(pluralName);
		if (children == null){
			children = new JSONArray();
			root.put(pluralName, children);
		}
		List<JSONObject> ret = new ArrayList<JSONObject>();
		for (Object o : children){
			if (o instanceof JSONObject){
				ret.add((JSONObject)o);
			}
		}
		return ret;
	}

	@Override
	public Set<String> getArrayElementNames() {
		Set<String> attr = new HashSet<String>();
		JSONObject obj = root;
		for (Object key : obj.keySet()){
			Object value = obj.get(key);
			if (value instanceof JSONObject){
				continue;
			}else if (value instanceof JSONArray){
				attr.add(StringUtil.valueOf(StringUtil.singularize(StringUtil.valueOf(key))));
			}else {
				continue;
			}
		}
		return attr;
	}

	@Override
	public void removeArrayElement(String name) {
		String pluralName = StringUtil.pluralize(name);
		root.remove(pluralName);
	}

	@Override
	public void setArrayElement(String name, List<JSONObject> elements) {
		String pluralName = StringUtil.pluralize(name);
		JSONArray array = new JSONArray();
		array.addAll(elements);
		root.put(pluralName,array);
	}

	@Override
	public Set<String> getElementAttributeNames() {
		Set<String> attr = new HashSet<String>();
		JSONObject obj = root;
		for (Object key : obj.keySet()){
			Object value = obj.get(key);
			if (value instanceof JSONObject){
				attr.add(StringUtil.valueOf(key));
			}else if (value instanceof JSONArray){
				continue;
			}else {
				continue;
			}
		}
		return attr;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void setAttribute(String name , String obj){
		if (obj != null){
			root.put(name,obj);
		}
	}

    @Override
    public void setElementAttribute(String name, String value) {
        setAttribute(name,value);
	}

    @SuppressWarnings("unchecked")
	@Override
	public JSONObject createElementAttribute(String name) {
		JSONObject attr = getElementAttribute(name);
		if (attr == null){
			attr = new JSONObject();
			root.put(name, attr);
		}
		return attr;
	}
	
	@Override
	public JSONObject getElementAttribute(String name) {
		JSONObject attr = (JSONObject)root.get(name);
		return attr;
	}

	@Override
	public void setAttribute(String name, JSONObject element) {
		if (element != null){
			root.put(name,element);
		}
	}

	public String toString(){
		StringWriter w =  new StringWriter();
		JSONFormatter formatter  = new JSONFormatter();
		try {
			formatter.writePrettyJson(root, w);
		}catch(IOException ex){
			throw new RuntimeException(ex);
		}
		return w.toString();
	}

	
	
	@Override
	public Set<String> getAttributes() {
		Set<String> attr = extractAttributes(root);
		return attr;
	}
	private Set<String> extractAttributes(JSONObject obj){
		Set<String> attr = new HashSet<String>();
		for (Object key : obj.keySet()){
			Object value = obj.get(key);
			if (value instanceof JSONObject){
				continue;
			}else if (value instanceof JSONArray){
				continue; 
			}
			attr.add(StringUtil.valueOf(key));
		}
		return attr;
	}

	@Override
	public String getAttribute(String name) {
		Object attr = root.get(name);
		if (attr == null){
			return null;
		}else if (attr instanceof JSONObject){
			return null; 
		}else if (attr instanceof JSONArray){
			return null;
		}else {
			return StringUtil.valueOf(attr);
		}
	}

	@Override
	public void removeElementAttribute(String name){
		root.remove(name);
	}
	@Override
	public void removeAttribute(String name){
		root.remove(name);
	}

}
