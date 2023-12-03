package com.venky.swf.plugins.lucene.index.common;

import com.venky.core.collections.SequenceSet;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;

import java.io.IOException;
import java.util.List;

public class CompleteSearchCollector extends SimpleCollector {

	List<Integer> docIds = new SequenceSet<>();
	public List<Integer> getDocIds(){
		return docIds;
	}
	@Override
	public void setScorer(Scorer scorer) throws IOException {

	}

	int docBase = 0;
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		docBase = context.docBase;
	}


	@Override
	public void collect(int doc) throws IOException {
		docIds.add(doc + docBase);
	}


	@Override
	public boolean needsScores() {
		return false;
	}
}
