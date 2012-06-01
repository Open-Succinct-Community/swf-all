package com.venky.swf.plugins.lucene.extensions;

import com.venky.extension.Registry;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;

public class UpdateDocumentExtension extends IndexExtension{
	static {
		Registry.instance().registerExtension("Model.after.update", new UpdateDocumentExtension());
	}

	@Override
	protected void updateIndex(LuceneIndexer indexer, Record record) {
		try {
			indexer.updateDocument(record);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
}
