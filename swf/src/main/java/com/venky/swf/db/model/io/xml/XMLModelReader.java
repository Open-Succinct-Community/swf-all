package com.venky.swf.db.model.io.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.AbstractModelReader;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

public class XMLModelReader<M extends Model> extends AbstractModelReader<M,XMLElement> {
	public XMLModelReader(Class<M> modelClass){
		super(modelClass);
	}
	@Override 
	public List<M> read(InputStream in) {
		List<M> ret = new ArrayList<M>();
		XMLDocument doc = XMLDocument.getDocumentFor(in);
		XMLElement root = doc.getDocumentRoot();
		if (root.getNodeName().equals(StringUtil.pluralize(root.getNodeName()))){
			for (Iterator<XMLElement> elemIterator = doc.getDocumentRoot().getChildElements() ; elemIterator.hasNext() ; ){
				XMLElement e = elemIterator.next();
				if (e.getNodeName().equals(getBeanClass().getSimpleName())) {
					ret.add(read(e));
				}
			}
		}else {
			ret.add(read(doc.getDocumentRoot()));
		}
		return ret;
	}
	
}
