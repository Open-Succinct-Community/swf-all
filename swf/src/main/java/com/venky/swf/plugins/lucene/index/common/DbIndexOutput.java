package com.venky.swf.plugins.lucene.index.common;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import org.apache.lucene.store.OutputStreamIndexOutput;

import java.io.IOException;

public class DbIndexOutput extends OutputStreamIndexOutput {
	public DbIndexOutput(DatabaseDirectory dir,String fileName){
		super("DbIndexOutput(path=\"" +dir.getModelDirectory().getName() + "\")",fileName, new IndexFileOutputStream(dir,fileName),1000);
	}

	public static class IndexFileOutputStream extends SeekableByteArrayOutputStream {
		DatabaseDirectory dir;
		String fileName;
		public IndexFileOutputStream(DatabaseDirectory dir, String fileName){
			this.dir = dir;
			this.fileName = fileName;
		}
		public IndexFile getFile(){
			IndexFile file = dir.getFile(fileName);
			if (file == null){
				file = Database.getTable(IndexFile.class).newRecord();
				file.setIndexDirectoryId(dir.getModelDirectory().getId());
				file.setName(fileName);
				file.save();
			}
			return file;
		}

		@Override
		public void close() throws IOException {
			super.close();
			IndexFile file  = getFile();
			byte[] array = toByteArray();
			file.setIndexContent(new ByteArrayInputStream(array));
			file.setLength(array.length);
			file.save();
		}
	}

	


}
