/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;

/**
 *
 * @author venky
 */
public class Control extends Properties implements _IControl{

    /**
     * 
     */
    private static final long serialVersionUID = 1464298453429814631L;
    private long nextId(){
    	return new Object().hashCode();
    }
    
    public void setWaterMark(String watermark){
    	if (!ObjectUtil.isVoid(watermark)){
        	setProperty("placeholder", watermark);
    	}else {
    		remove("placeholder");
    	}
    }

    public void setToolTip(String tip){
    	if (!ObjectUtil.isVoid(tip)){
        	setProperty("title", tip);
    	}else {
    		remove("title");
    	}
    }

    public Control(String tag, String... pairs) {
        this.tag = tag;
        addClass(getClass().getSimpleName().toLowerCase());
        setProperty("id", String.valueOf(nextId()));

        Properties p = ObjectUtil.createProperties(true, pairs);
        putAll(p);
    }
    
    SequenceSet<String> classes = new SequenceSet<String>();
    public void setClass(String className){
    	classes.clear();
    	addClass(className);
    }
    public void addClass(String className){
    	classes.add(className);
    	StringBuilder classNames = new StringBuilder();
    	for (String cn: classes){
    		classNames.append(cn);
    		classNames.append(" ");
    	}
    	classNames.setLength(classNames.length()-1);
    	setProperty("class", classNames);
    }

    public void setProperty(String name, Object value) {
        super.setProperty(name, StringUtil.valueOf(value));
    }
    private String tag = null;
    private _IControl parent = null;

    public void setId(String id) {
        setProperty("id", id);
    }

    public String getId() {
        return getProperty("id");
    }
    private List<_IControl> containedControls = new ArrayList<_IControl>();

    public _IControl getParent() {
        return parent;
    }

    public String getTag() {
        return tag;
    }

    public void setParent(_IControl parent) {
        this.parent = parent;
    }

    public void addControl(int index,_IControl control){
    	containedControls.add(index,control);
        control.setParent(this);
    }
    public void addControl(_IControl control) {
        addControl(containedControls.size(), control);
    }

    public List<_IControl> getContainedControls() {
        return Collections.unmodifiableList(containedControls);
    }
    
    public _IControl removeContainedControlAt(int index){
    	return containedControls.remove(index);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (getTag() != null){
            b.append("<").append(getTag()).append(" ");
            for (Object key : keySet()) {
                String pvalue = getProperty(key.toString());
                b.append(key).append("=\"").append(pvalue).append("\" ");
            }
            b.append(">");
        }
        b.append(getText());
        for (_IControl contained : containedControls) {
            b.append(contained);
        }
        if (getTag() != null){
            b.append("</").append(getTag()).append(">");
        }
        
        return b.toString();
    }
    private String text = "";

    public String getText() {
        return text;
    }

    public void setText(String value) {
        this.text = value;
    }
    
    public String getName(){ 
        return getProperty("name");
    }

    public void setName(final String name){
        setProperty("name", name);
    }

    public void setValue(final Object value){
        setProperty("value", StringEscapeUtils.escapeHtml4(StringUtil.valueOf(value)));
    }
    
    public String getValue(){
        return getProperty("value");
    }
    public void setEnabled(final boolean enabled){
        if (enabled){
            remove("disabled");
        }else {
            setProperty("disabled", !enabled);
        }
    }
    
    public void setReadOnly(final boolean readonly){
    	if (readonly){
    		setProperty("readonly","readonly");
    	}else{
    		remove("readonly");
    	}
    }
    public boolean isReadOnly(){
    	return containsKey("readonly");
    }
    public boolean isEnabled(){
        return !containsKey("disabled");
    }

    public void setVisible(final boolean visible){
    	addClass("hidden");
    }

    public boolean isVisible(){ 
        return !getProperty("class").contains(" hidden ");
    }
    
    @SuppressWarnings("unchecked")
	public static <T extends _IControl> void hunt(_IControl control, Class<T> controlClass,List<T> hunted){
		if (controlClass.isInstance(control)){
			hunted.add((T)control);
		}else {
			for (_IControl c :control.getContainedControls()){
				hunt(c,controlClass,hunted);
			}
		}
	}
}
