package com.venky.swf.integration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.venky.core.string.StringUtil;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

public class XML extends FormatHelper<XMLElement>{
	XMLElement root = null;
	public XML(InputStream in){
		this(XMLDocument.getDocumentFor(in).getDocumentRoot());
	}
	
	public XML(XMLElement root){
		this.root = root;
	}
	
	public XML(String rootName, boolean isPlural) {
		super();
		//Plural or singular makes no diff to XML
		root = new XMLDocument(rootName).getDocumentRoot();
	}

	@Override
	public XMLElement getRoot() {
		return root;
	}
	
	@Override
	public XMLElement createChildElement(String name) {
		String plural = StringUtil.pluralize(name);
		XMLElement pluralElement = null;
		if (root.getNodeName().equals(plural)){
			pluralElement = root;
		}else {
			pluralElement = root.getChildElement(plural);
			if (pluralElement == null){
				pluralElement = root.createChildElement(plural);
			}
		}		
		return pluralElement.createChildElement(name);
	}
	
	@Override
	public List<XMLElement> getChildElements(String name){
		String plural = StringUtil.pluralize(name);
		XMLElement pluralElement = null;
		if (root.getNodeName().equals(plural)){
			pluralElement = root;
		}else {
			pluralElement = root.getChildElement(plural);
			if (pluralElement == null){
				pluralElement = root.createChildElement(plural);
			}
		}
		List<XMLElement> ret = new ArrayList<XMLElement>();
		Iterator<XMLElement> ei = pluralElement.getChildElements(name);
		while (ei.hasNext()){
			ret.add(ei.next());
		}
		return ret;
	}
	@Override
	public void setAttribute(String name, String value) {
		if (value != null){
			root.setAttribute(name, value);
		}
	}

	
	@Override
	public XMLElement createElementAttribute(String name) {
		XMLElement element = getElementAttribute(name);
		if (element == null){
			element = root.createChildElement(name);
		}
		return element;
	}

	public String toString(){
		return root.toString();
	}

	@Override
	public Set<String> getAttributes() {
		return root.getAttributes().keySet();
	}

	@Override
	public String getAttribute(String name) {
		return root.getAttribute(name);
	}

	@Override
	public XMLElement getElementAttribute(String name) {
		return root.getChildElement(name);
	}
	
	
}
