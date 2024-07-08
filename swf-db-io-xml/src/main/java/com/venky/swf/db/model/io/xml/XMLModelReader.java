package com.venky.swf.db.model.io.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.AbstractModelReader;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

public class XMLModelReader<M extends Model> extends AbstractModelReader<M,XMLElement> {
	public XMLModelReader(Class<M> modelClass){
		super(modelClass);
	}
	@Override 
	public List<M> read(InputStream in,boolean saveRecursive) {
		return read(in, FormatHelper.change_case(getBeanClass().getSimpleName(), KeyCase.CAMEL, Config.instance().getApiKeyCase()),saveRecursive);
	}
	@Override
	public List<M> read(InputStream in,String rootElementName,boolean saveRecursive) {
		List<M> ret = new ArrayList<M>();
		XMLDocument doc = XMLDocument.getDocumentFor(in);
		XMLElement root = doc.getDocumentRoot();
		if (root.getNodeName().equals(rootElementName)){
			ret.add(read(root,saveRecursive));
		}else {
			Iterator<XMLElement> pluralRootElementIterator = root.getChildElements(StringUtil.pluralize(rootElementName));
		
			if (pluralRootElementIterator.hasNext()){
				root = pluralRootElementIterator.next();
			}

			if (pluralRootElementIterator.hasNext()){
				throw new RuntimeException("Don't know how to read document with multiple pluralized elements" + StringUtil.pluralize(rootElementName) );
			}
			
			for (Iterator<XMLElement> elemIterator = root.getChildElements(rootElementName) ; elemIterator.hasNext() ; ){
				XMLElement e = elemIterator.next();
				if (e.getNodeName().equals(rootElementName)) {
					ret.add(read(e,saveRecursive));
				}
			}
		}
		
		return ret;
	}
}
