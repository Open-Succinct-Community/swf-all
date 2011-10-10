/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author venky
 */
public class Control extends Properties {

    /**
     * 
     */
    private static final long serialVersionUID = 1464298453429814631L;
    private long nextId(){
    	return new Object().hashCode();
    }

    public Control(String tag, String... pairs) {
        this.tag = tag;
        setProperty("class", getClass().getSimpleName().toLowerCase());
        setProperty("id", String.valueOf(nextId()));

        Properties p = ObjectUtil.createProperties(true, pairs);
        putAll(p);
    }
    
    public void addClass(String className){
    	setProperty("class", getProperty("class") + " " + className);
    }

    public void setProperty(String name, Object value) {
        super.setProperty(name, StringUtil.valueOf(value));
    }
    private String tag = null;
    private Control parent = null;

    public void setId(String id) {
        setProperty("id", id);
    }

    public String getId() {
        return getProperty("id");
    }
    private List<Control> containedControls = new ArrayList<Control>();

    public Control getParent() {
        return parent;
    }

    public String getTag() {
        return tag;
    }

    protected void setParent(Control parent) {
        this.parent = parent;
    }

    public void addControl(Control control) {
        containedControls.add(control);
        control.setParent(this);
    }

    public List<Control> getContainedControls() {
        return Collections.unmodifiableList(containedControls);
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
        for (Control contained : containedControls) {
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
}
