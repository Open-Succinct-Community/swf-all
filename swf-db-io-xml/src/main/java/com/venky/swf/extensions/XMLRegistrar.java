package com.venky.swf.extensions;

import java.io.InputStream;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.io.ModelReaderFactory;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.io.ModelWriterFactory;
import com.venky.swf.db.model.io.xml.XMLModelReader;
import com.venky.swf.db.model.io.xml.XMLModelWriter;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelperBuilder;
import com.venky.swf.integration.XML;
import com.venky.xml.XMLElement;

public class XMLRegistrar {
	static {
		FormatHelper.registerFormat(MimeType.APPLICATION_XML, XMLElement.class, new FormatHelperBuilder<XMLElement>(){

			@Override
			public FormatHelper<XMLElement> constructFormatHelper(InputStream in) {
				return new XML(in);
			}

			@Override
			public FormatHelper<XMLElement> constructFormatHelper(String root,
					boolean isPlural) {
				return new XML(root,isPlural);
			}

			@Override
			public FormatHelper<XMLElement> constructFormatHelper(
					XMLElement rootElement) {
				return new XML(rootElement);
			}
			
		});
		
		ModelIOFactory.registerIOFactories(XMLElement.class, new ModelReaderFactory<XMLElement>() {

			@Override
			public <M extends Model> ModelReader<M, XMLElement> createModelReader(
					Class<M> modelClass) {
				return new XMLModelReader<M>(modelClass);
			}
		}, new ModelWriterFactory<XMLElement>() {

			@Override
			public <M extends Model> ModelWriter<M, XMLElement> createModelWriter(
					Class<M> modelClass) {
				return new XMLModelWriter<M>(modelClass);
			}
		});
	}
}
