package com.venky.swf.integration;

import java.io.InputStream;
import java.util.*;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;
import org.w3c.dom.Node;

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
    public void setElementAttribute( String name, String value) {
	    XMLElement element = createElementAttribute(name);
	    element.setNodeValue(value);
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
	    Set<String> attributes = new HashSet<>();
		attributes.addAll(root.getAttributes().keySet());
		for (Iterator<XMLElement> i =  root.getChildElements() ; i.hasNext() ; ){
		    XMLElement element = i.next();
		    if (!element.hasAttributes() && !element.getChildElements().hasNext() && (!ObjectUtil.isVoid(element.getNodeValue()) || !ObjectUtil.isVoid(element.getCharacterData()))){
                attributes.add(element.getTagName());
            }
        }
        return attributes;
	}


	@Override
	public String getAttribute(String name) {
		String attr = null;
		if (root.hasAttribute(name)){
		    attr = root.getAttribute(name);
        }else {
		    XMLElement attrElement = getElementAttribute(name);
		    if (attrElement != null && !attrElement.hasAttributes() && !attrElement.getChildElements().hasNext()){
		        attr = attrElement.getNodeValue();
		        if (ObjectUtil.isVoid(attr)) {
		            attr = attrElement.getCharacterData();
                }
            }
        }
        return attr;
	}

	@Override
	public XMLElement getElementAttribute(String name) {
		return root.getChildElement(name);
	}

	@Override
	public void setAttribute(String name, XMLElement element) {
		if (element != null && ObjectUtil.equals(element.getTagName(),name)){
			if (element.getOwnerDocument() == root.getOwnerDocument()){
				root.appendChild(element.cloneElement(true));
			}else {
				root.appendChild(element);
			}
		}
	}

	@Override
	public void removeElementAttribute(String name){
		root.removeChild(root.getChildElement(name));
	}
	@Override
	public void removeAttribute(String name){
		root.removeAttribute(name);
	}
}
