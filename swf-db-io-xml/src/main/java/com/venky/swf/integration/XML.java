package com.venky.swf.integration;

import java.io.InputStream;
import java.util.*;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

public class XML extends FormatHelper<XMLElement>{
	XMLElement root = null;
	public XML(InputStream in){
		this(XMLDocument.getDocumentFor(in).getDocumentRoot());
		fixInputCase();
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
	public String getRootName() {
		return root.getTagName();
	}

	@Override
	public XMLElement changeRootName(String toName) {
		if (root.getTagName().equals(toName)){
			return root;
		}
		XMLElement parent = root.getParentElement();
		XMLDocument document = root.getXMLDocument();
		final XMLElement clone ;
		if (parent == null){
			document.remove(root);
			clone = document.createElement(toName);
			document.append(clone);
		}else {
			parent.removeChild(root);
			clone = parent.createChildElement(toName);
		}

		root.getAttributes().forEach((n,v)-> clone.setAttribute(n,v));
		root.getChildElements().forEachRemaining(e-> clone.appendChild(e));
		root = clone;
		return root;
	}

	@Override
	public XMLElement createArrayElement(String name) {
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
	public List<XMLElement> getArrayElements(String name){
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
	public Set<String> getArrayElementNames() {
		Set<String> attributes = new HashSet<>();
		for (Iterator<XMLElement> i =  root.getChildElements() ; i.hasNext() ; ){
			XMLElement element = i.next();
			String tagName = element.getTagName();

			if (ObjectUtil.isVoid(element.getNodeValue())){
				if (tagName.equals(StringUtil.pluralize(tagName))) {
					String singularTagName = StringUtil.singularize(tagName);

					Iterator<XMLElement> elementIterator = element.getChildElements();
					if (!elementIterator.hasNext()) {
						attributes.add(singularTagName);
					} else if (elementIterator.next().getTagName().equals(singularTagName)) {
						attributes.add(singularTagName);
					}
				}else if (tagName.equals(StringUtil.singularize(tagName)) && root.getTagName().equals(StringUtil.pluralize(tagName))){
					attributes.add(tagName);
					break;
				}
			}
		}
		return attributes;
	}

	@Override
	public void removeArrayElement(String name) {
		XMLElement arrayElement = root.getChildElement(StringUtil.pluralize(name));
		if (arrayElement != null) {
			root.removeChild(arrayElement);
		}
	}

	@Override
	public void setArrayElement(String name, List<XMLElement> elements) {
		String pluralName = StringUtil.pluralize(name);
		XMLElement plural = root.getTagName().equals(pluralName) ? root : root.getChildElement(pluralName,true);
		for (XMLElement element : elements){
			plural.appendChild(element);
		}
	}

	@Override
	public Set<String> getElementAttributeNames() {
		Set<String> attributes = new HashSet<>();
		for (Iterator<XMLElement> i =  root.getChildElements() ; i.hasNext() ; ){
			XMLElement element = i.next();
			if (ObjectUtil.isVoid(element.getNodeValue()) &&
					element.getTagName().equals(StringUtil.singularize(element.getTagName())) &&
					!root.getTagName().equals(StringUtil.pluralize(element.getTagName()))){
				attributes.add(element.getTagName());
			}
		}
		return attributes;
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
		fixOutputCase();
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
		if (element != null ){
			if (!ObjectUtil.equals(element.getTagName(),name)) {
				new XML(element).changeRootName(name);
			}else {
				if (element.getOwnerDocument() == root.getOwnerDocument()){
					root.appendChild(element.cloneElement(true));
				}else {
					root.appendChild(element);
				}
			}
		}
	}

	@Override
	public void removeElementAttribute(String name){
		XMLElement child = root.getChildElement(name);
		if (child != null) {
			root.removeChild(child);
		}
	}
	@Override
	public void removeAttribute(String name){
		root.removeAttribute(name);
	}
}
