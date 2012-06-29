package com.venky.swf.plugins.lucene.extensions;

import java.io.IOException;

import com.venky.extension.Registry;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;

public class DestroyDocumentExtension extends IndexExtension{
	static {
		Registry.instance().registerExtension("Model.after.destroy", new DestroyDocumentExtension());
	}

	@Override
	protected void updateIndex(LuceneIndexer indexer, Record record) {
		try {
			indexer.removeDocument(record);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

}
