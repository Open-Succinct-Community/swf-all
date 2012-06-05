package com.venky.swf.plugins.lucene.index.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.SingleInstanceLockFactory;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class DatabaseDirectory extends Directory {
	
	public DatabaseDirectory() throws IOException{
		this("MODEL");
	}
	
	public DatabaseDirectory(String tableName) throws IOException{
		super();
		ModelReflector<IndexDirectory> ref = ModelReflector.instance(IndexDirectory.class);
		List<IndexDirectory> dirs = new Select().from(IndexDirectory.class).where(new Expression(ref.getColumnDescriptor("NAME").getName(),Operator.EQ,tableName)).execute(IndexDirectory.class);
		if (dirs.size() == 1){
			directory = dirs.get(0);
		}
		if (directory == null){
			throw new FileNotFoundException("Directory entry missing for " + tableName +". Check if there are any field getters marked with annotation @Index in any of it's models.");
		}
		setLockFactory(new SingleInstanceLockFactory());
	}
	
	private IndexDirectory directory = null;
	IndexDirectory getModelDirectory() {
		return directory;
	}
	
	@Override
	public String[] listAll() throws IOException {
		return getFileNames().toArray(new String[] {});
	}

	@Override
	public boolean fileExists(String name) throws IOException {
		return getFileNames().contains(name);
	}

	@Override
	public long fileModified(String name) throws IOException {
		Select sel = new Select().from(IndexFile.class).where(
				new Expression("NAME", Operator.EQ, name));
		List<IndexFile> files = sel.execute();
		if (!files.isEmpty()) {
			IndexFile file = files.get(0);
			return file.getUpdatedAt().getTime();
		} else {
			throw new FileNotFoundException();
		}
	}

	private List<IndexFile> getFiles() throws IOException {
		return getModelDirectory().getIndexFiles();
	}

	private Set<String> getFileNames() throws IOException {
		List<IndexFile> files = getFiles();
		Set<String> names = new HashSet<String>();
		for (IndexFile file : files) {
			names.add(file.getName());
		}
		return names;
	}

	public IndexFile getFile(String name) {
		Select sel = new Select().from(IndexFile.class).where(
				new Expression("NAME", Operator.EQ, name));
		List<IndexFile> files = sel.execute();
		IndexFile file = null;
		if (!files.isEmpty()) {
			file = files.get(0);
		}
		return file;
	}

	@Override
	public void touchFile(String name) throws IOException {
		IndexFile file = getFile(name);
		if (file == null) {
			throw new FileNotFoundException();
		}
		file.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
		file.save();
	}

	@Override
	public void deleteFile(String name) throws IOException {
		IndexFile file = getFile(name);
		if (file == null) {
			throw new FileNotFoundException();
		}
		file.destroy();
	}

	@Override
	public long fileLength(String name) throws IOException {
		IndexFile file = getFile(name);
		if (file == null) {
			throw new FileNotFoundException();
		}
		return file.getLength();
	}


	@Override
	public IndexOutput createOutput(String name) throws IOException {
		return new DbIndexOutput(this,name);	
	}

	@Override
	public IndexInput openInput(String name) throws IOException {
		final IndexFile file = getFile(name);
		if (file == null) {
			throw new FileNotFoundException(name);
		}
		return new DbIndexInput(file);
	}

	@Override
	public void close() throws IOException {
		isOpen = false;
		directory = null;
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		try {
			Database.getInstance().getCurrentTransaction().commit();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
