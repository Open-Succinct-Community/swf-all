package com.venky.swf.plugins.lucene.index.common;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.store.BufferedIndexInput;

import com.venky.swf.plugins.lucene.db.model.IndexFile;

public class DbIndexInput extends BufferedIndexInput{
	
	IndexFile file = null;
	InputStream is = null;
	public DbIndexInput(IndexFile file){
		super(file.getName(),(int)file.getLength());
		this.file = file;
		this.is = file.getIndexContent();
	}

	@Override
	protected void readInternal(byte[] b, int offset, int length)
			throws IOException {
		is.read(b, offset, length);
	}

	@Override
	protected void seekInternal(long pos) throws IOException {
		is.reset();
		is.skip(pos);
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public long length() {
		return file.getLength();
	}

}
