/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

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

    public void setForm(String formId){
    	setProperty("form", formId);
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
    protected final String getDefaultCssClass(){
    	return LowerCaseStringCache.instance().get(
    			StringUtil.underscorize(getClass().getSimpleName()).replace('_','-'));
    }
    public Control(String tag, String... pairs) {
        this.tag = tag;
        setProperty("id", "id_"+String.valueOf(nextId()));
        Properties p = ObjectUtil.createProperties(true, pairs);
        putAll(p);
        addClass(getDefaultCssClass());
    }
    
    SequenceSet<String> classes = new SequenceSet<String>();
    public void setClass(String className){
    	classes.clear();
    	addClass(className);
    }
    public void removeClass(String className){
    	StringTokenizer tok = new StringTokenizer(className);
    	while (tok.hasMoreTokens()){
        	classes.remove(tok.nextToken());
    	}
    	finalizeClassAttribute();
    }
    private void finalizeClassAttribute(){
    	StringBuilder classNames = new StringBuilder();
    	for (String cn: classes){
    		classNames.append(cn);
    		classNames.append(" ");
    	}
    	if (classNames.length() > 0) {
    		classNames.setLength(classNames.length()-1);
        	setProperty("class", classNames);
    	}else {
    		remove("class");
    	}
    }
    public void addClass(String className){
    	StringTokenizer tok = new StringTokenizer(className);
    	while (tok.hasMoreTokens()){
        	classes.add(tok.nextToken());
    	}
    	finalizeClassAttribute();
    }
    
    public void setProperty(String name , Object value){
        this.setProperty(name,StringUtil.valueOf(value));
    }
    @Override
    public Object setProperty(String name, String value) {
    	return super.setProperty(name, Encode.forHtmlAttribute(value));
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
    
    protected void setTag(String tag){
    	this.tag = tag;
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
    public void removeControl(_IControl control){
    	containedControls.remove(control);
    	control.setParent(null);
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
        boolean closeEmptyTagMinimally = useMinimizedTagSyntax() && isTagEmpty() ;
        if (getTag() != null){
            b.append("<").append(getTag()).append(" ");
            for (Object key : keySet()) {
                String pvalue = getProperty(key.toString());
                b.append(key).append("=\"").append(pvalue).append("\" ");
            }
            if (closeEmptyTagMinimally){
            	b.append("/");
            }
            b.append(">");
        }
        if (!closeEmptyTagMinimally){
	        b.append(getText());
	        for (_IControl contained : containedControls) {
	            b.append(contained);
	        }
	        if (getTag() != null){
	            b.append("</").append(getTag()).append(">");
	        }
        }
        return b.toString();
    }
    protected boolean useMinimizedTagSyntax(){
    	return false;
    }
    protected boolean isTagEmpty(){
    	return (ObjectUtil.isVoid(getText()) && getContainedControls().isEmpty());
    }
    
    private String text = "";

    public String getText() {
        return text;
    }
    public void setText(String value) {
        this.setText(value,true);
    }
    public void setText(String value,boolean escapeHtml) {
        if (escapeHtml){
            this.text = Encode.forHtmlContent(value);
        }else{
            this.text = value;
        }
    }
    
    public String getName(){ 
        return getProperty("name");
    }

    public void setName(final String name){
        setProperty("name", name);
    }

    String value;
    public void setValue(final Object value){
        this.value = StringUtil.valueOf(value);
        setProperty("value", this.value);
    }
    public String getUnescapedValue(){
    	return value;
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
    	if (visible){
    		removeClass("d-none");
    	}else {
    		addClass("d-none");
    	}
    }

    public boolean isVisible(){ 
        return !classes.contains("d-none");
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
