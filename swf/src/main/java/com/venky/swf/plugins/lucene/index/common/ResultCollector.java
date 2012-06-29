package com.venky.swf.plugins.lucene.index.common;

import org.apache.lucene.document.Document;

public interface ResultCollector {
	public void found(Document doc);
}
