package com.venky.swf.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.StringArrayConverter;
import com.venky.swf.db.model.io.json.JSONFormatter;
import com.venky.swf.routing.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.venky.core.string.StringUtil;


public class JSON extends FormatHelper<JSONObject>{

	private JSONObject root = null;
	public JSON(InputStream in) {
		this(parseWithException(in));
	}
	private static JSONAware parseWithException(InputStream in){
		try {
			return (JSONAware) JSONValue.parse(new InputStreamReader(in));
		}catch (Exception ex){
			throw new RuntimeException(ex);
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

	public JSON(JSONAware obj){
		if (obj instanceof JSONObject) {
			this.root = (JSONObject) obj;
		}else {
			this.root = new JSONObject();
			root.put("Root",obj);
		}
		fixInputCase();
	}


	@Override
	public void setRoot(JSONObject root) {
		this.root = root;
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
		try {
			return (JSONObject) root.get(name);
		}catch (ClassCastException ex){
			Config.instance().getLogger(getClass().getName()).log(Level.WARNING,String.format("%s is not a JSONObject in %s", name , root.toString()));
			throw ex;
		}
}

	@Override
	public void setAttribute(String name, JSONObject element) {
		if (element != null){
			root.put(name,element);
		}
	}

	public String toString(){
		fixOutputCase();
		StringWriter w =  new StringWriter();
		JSONFormatter formatter  = new JSONFormatter();
		if (getRootName() == null || !isPlural() || isRootElementNameRequired()) {
			formatter.writePrettyJson(root, w);
		}else {
			formatter.writePrettyJsonArray((JSONArray) root.get(getRootName()),w);
		}
		return w.toString();
	}

	
	
	@Override
	public Set<String> getAttributes() {
		return extractAttributes(root);
	}

	@Override
	public boolean isArrayAttribute(String name) {
		Object attr = root.get(name);
		if ( attr == null){
			return false;
		}
		if (!(attr instanceof JSONArray)){
			return false;
		}
		return !(((JSONArray)attr).get(0) instanceof JSONAware);
	}

	private Set<String> extractAttributes(JSONObject obj){
		Set<String> attr = new HashSet<>();
		for (Object key : obj.keySet()){
			Object value = obj.get(key);
			if (value instanceof JSONAware){
				if (value instanceof JSONArray){
					JSONArray array = (JSONArray) value;
					if (!array.isEmpty()){
						if (!(array.get(0) instanceof JSONAware)){
							attr.add(StringUtil.valueOf(key));
						}
					}
				}
			}else {
				attr.add(StringUtil.valueOf(key));
			}
		}
		return attr;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> P getAttribute(String name) {
		Object attr = root.get(name);
		if (attr == null){
			return null;
		}else if (attr instanceof JSONAware){
			if (isArrayAttribute(name)){
				JSONArray array = ((JSONArray) attr);
				String[] value = new String[array.size()];
				for (int i = 0; i < array.size(); i++) {
					value[i] = StringUtil.valueOf(array.get(i));
				}
				return (P)value;
			}
		}else {
			return (P)StringUtil.valueOf(attr);
		}
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		return root.containsKey(name);
	}


	@Override
	public void setAttribute(String name, String[] value) {
		JSONArray array = new JSONArray() ;
		root.put(name,array);
		array.addAll(Arrays.asList(value));
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
