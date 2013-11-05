package com.venky.swf.db.model.io.xml; 

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.AbstractModelWriter;
import com.venky.xml.XMLElement;

public class XMLModelWriter<M extends Model> extends AbstractModelWriter<M, XMLElement>{ 
	public XMLModelWriter(Class<M> beanClass) {
		super(beanClass);
	}

}
