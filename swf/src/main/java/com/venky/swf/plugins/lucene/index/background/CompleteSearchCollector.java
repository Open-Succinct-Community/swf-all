package com.venky.swf.plugins.lucene.index.background;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

public class CompleteSearchCollector extends Collector{

	private int docBase = 0 ;
	List<Integer> docIds = new ArrayList<Integer>();
	public List<Integer> getDocIds(){
		return docIds;
	}
	@Override
	public void setScorer(Scorer scorer) throws IOException {
	}

	@Override
	public void collect(int doc) throws IOException {
		docIds.add(docBase + doc);
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase)
			throws IOException {
		this.docBase = docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

}
