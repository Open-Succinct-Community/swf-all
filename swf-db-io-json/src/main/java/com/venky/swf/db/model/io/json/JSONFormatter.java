package com.venky.swf.db.model.io.json;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by venky on 11/5/16.
 */
public class JSONFormatter  {
	public JSONFormatter(){
		this(2);
	}
    public JSONFormatter(int spacing){
    	SPACES=spacing;
    }
    public void writePrettyJsonArray(JSONArray jsonArray, Writer w) throws IOException{
    	w.append("[");
    	for (int i = 0 ; i < jsonArray.size() ; i ++ ){
    		Object o = jsonArray.get(i);
            if (i > 0){
                w.append(",");
            }
    		writeObject(o, w);
    	}
    	w.append("]");
    }
    public void writePrettyJson(JSONObject obj,Writer w) throws IOException{
    	w.append("{");
    	writeAttributes(obj,w);
    	w.append("}");
    }
    private int indent = 0;
    private int SPACES = 2;
    private void indent(Writer w) throws IOException{
    	indent += SPACES;
    	for (int i = 0 ; i< indent ; i ++){
			w.append(" ");
		}
    }
    private void backIndent(Writer w) throws IOException {
    	indent -=SPACES;
    }
    private void writeObject(Object o ,Writer w) throws IOException{
    	if (o instanceof JSONObject){
    		writePrettyJson((JSONObject)o, w);
    	}else if (o instanceof JSONArray){
    		writePrettyJsonArray((JSONArray)o, w);
    	}else {
    		w.append('"').append(JSONObject.escape(o.toString())).append("\"");
    	}
    	
    }
	private void writeAttributes(JSONObject obj, Writer w) throws IOException {
		boolean first = true;
		List<String> keys = new ArrayList<String>();
		keys.addAll(obj.keySet());
		Collections.sort(keys);
		for (Object k : keys){
			Object v = obj.get(k);
            w.append("\n");
            indent(w);
            if (!first) {
                w.append(",");
            }else {
                first=false;
            }
			w.append("\"").append(JSONObject.escape(k.toString())).append("\" : ");
			if (v instanceof JSONObject){
				writePrettyJson((JSONObject)v, w);
			}else if (v instanceof JSONArray) {
				writePrettyJsonArray((JSONArray)v, w);
			}else {
				w.append('"').append(JSONObject.escape(v.toString())).append("\"");
			}
			backIndent(w);
		}
        w.append("\n");
        backIndent(w);
        indent(w);
	}
    
    
}
