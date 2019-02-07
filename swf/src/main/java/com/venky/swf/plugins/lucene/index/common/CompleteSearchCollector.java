package com.venky.swf.plugins.lucene.index.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;

public class CompleteSearchCollector extends SimpleCollector {

	List<Integer> docIds = new ArrayList<>();
	public List<Integer> getDocIds(){
		return docIds;
	}
	@Override
	public void setScorer(Scorer scorer) throws IOException {

	}

	@Override
	public void collect(int doc) throws IOException {
		docIds.add(doc);
	}


	@Override
	public boolean needsScores() {
		return false;
	}
}
