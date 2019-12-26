package com.venky.swf.plugins.lucene.index.common;

import org.apache.lucene.document.Document;

public interface ResultCollector {
	public boolean found(Document doc);
}
