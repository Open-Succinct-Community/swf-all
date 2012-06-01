package com.venky.swf.plugins.lucene.index;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import com.venky.swf.sql.Select;

public class DatabaseDirectoryTest {
	@Before
	public void openDB(){
		try {
			Database db = Database.getInstance(true);
			Select sel = new Select().from(IndexFile.class);
			for (IndexFile f : sel.execute(IndexFile.class) ){
				f.destroy();
			}
			db.getCurrentTransaction().commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	@Test
	public void test() throws IOException, ParseException {
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

		// 1. create the index
		Directory index = new DatabaseDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_30,
				analyzer);

		IndexWriter w = new IndexWriter(index, config);
		addDoc(w, "Lucene in Action");
		addDoc(w, "Lucene for Dummies");
		addDoc(w, "Managing Gigabytes");
		addDoc(w, "The Art of Computer Science");
		w.close();

		// 2. query
		String querystr = "Action";

		// the "title" arg specifies the default field to use
		// when no field is explicitly specified in the query.
		Query q = new QueryParser(Version.LUCENE_30, "title", analyzer)
				.parse(querystr);

		// 3. search
		int hitsPerPage = 10;
		IndexReader reader = IndexReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display results
		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("title"));
		}

		// searcher can only be closed when there
		// is no need to access the documents any more.
		searcher.close();
	}

	private static void addDoc(IndexWriter w, String value) throws IOException {
		Document doc = new Document();
		doc.add(new Field("title", value, Field.Store.YES, Field.Index.ANALYZED));
		w.addDocument(doc);
	}
}
