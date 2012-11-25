package com.venky.swf.integration;

import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.venky.core.string.StringUtil;


public class JSON extends FormatHelper<JSONObject>{

	private JSONObject root = null;
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
	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject createChildElement(String name){
		String pluralName = StringUtil.pluralize(name);
		JSONArray children = (JSONArray)root.get(pluralName);
		if (children == null){
			children = new JSONArray();
			root.put(pluralName, children);
		}
		
		JSONObject childHolder = new JSONObject();
		JSONObject child = new JSONObject();
		childHolder.put(name,child);
		children.add(childHolder);
		return child;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setAttribute(String name , String obj){
		if (obj != null){
			root.put(name,obj);
		}
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
	
	public String toString(){
		return root.toString();
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

	
	
}
