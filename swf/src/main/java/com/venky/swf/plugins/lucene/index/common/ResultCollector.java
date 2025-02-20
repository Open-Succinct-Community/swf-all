package com.venky.swf.plugins.lucene.index.common;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

public interface ResultCollector {
	public void collect(Document doc, ScoreDoc scoreDoc);
	
	boolean isEnough();
}
