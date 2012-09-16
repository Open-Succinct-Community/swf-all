package com.venky.swf.plugins.lucene.index.common;

import java.io.IOException;

import org.apache.lucene.store.BufferedIndexOutput;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.lucene.db.model.IndexFile;

public class DbIndexOutput extends BufferedIndexOutput{
	private final SeekableByteArrayOutputStream out ;
	private final DatabaseDirectory dir ;
	private final String fileName ;
	public DbIndexOutput(DatabaseDirectory dir,String fileName){
		this.dir = dir;
		this.fileName = fileName;
		this.out = new SeekableByteArrayOutputStream();
	}
	@Override
	protected void flushBuffer(byte[] b, int offset, int len)
			throws IOException {
		if (len > 0){
			out.write(b, offset, len);
			
			ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
			IndexFile file = getFile(fileName); 
			file.setIndexContent(bais);
			file.setLength(out.size());
			file.save();
		}
	}
	
	public IndexFile getFile(String name){
		IndexFile file = dir.getFile(fileName); 
		if (file == null){
			file = Database.getTable(IndexFile.class).newRecord();
			file.setIndexDirectoryId(dir.getModelDirectory().getId());
			file.setName(fileName);
			file.save();
		}
		return file;
	}
	
	public void seek(long pos) throws IOException {
		super.seek(pos);
		out.seek(pos);
	}

	@Override
	public long length() throws IOException {
		IndexFile file = dir.getFile(fileName);
		if (file == null){
			return 0 ;
		}
		return file.getLength();
	}
	
}
