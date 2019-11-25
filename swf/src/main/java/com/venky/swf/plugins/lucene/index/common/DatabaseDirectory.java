package com.venky.swf.plugins.lucene.index.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.SingleInstanceLockFactory;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class DatabaseDirectory extends BaseDirectory {
	
	public DatabaseDirectory() throws IOException{
		this("MODEL");
	}
	public static IndexDirectory getIndexDirectory(String tableName) throws  FileNotFoundException{
		return getIndexDirectory(tableName,false);
	}
	public static IndexDirectory getIndexDirectory(String tableName,boolean aggressivelyLock) throws  FileNotFoundException{
		ModelReflector<IndexDirectory> ref = ModelReflector.instance(IndexDirectory.class);
		List<IndexDirectory> dirs = new Select(true).from(IndexDirectory.class).where(new Expression(ref.getPool(),ref.getColumnDescriptor("NAME").getName(),Operator.EQ,tableName)).execute(IndexDirectory.class);
		if (dirs.size() == 1){
			return dirs.get(0);
		}
		throw new FileNotFoundException("Directory entry missing for " + tableName +". Check if there are any field getters marked with annotation @Index in any of it's models.");
	}

	public DatabaseDirectory(String tableName) throws IOException{
		super(new DatabaseLockFactory(getIndexDirectory(tableName)));
	}
	public IndexDirectory getModelDirectory(){
		return ((DatabaseLockFactory)lockFactory).getDirectory();
	}
	@Override
	public String[] listAll() throws IOException {
		return getFileNames().toArray(new String[] {});
	}

	@Override
	public void deleteFile(String name) throws IOException {
		IndexFile file = getFile(name);

		if (file == null) {
            throw new FileNotFoundException();
        }
        if (!file.getRawRecord().isNewRecord() && file.getId() > 0 ) {
        	file = Database.getTable(IndexFile.class).lock(file.getId());
        	if (file != null) {
				file.destroy();
			}
        }
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
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		return new DbIndexOutput(this,name);
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		String tmpName = prefix + UUID.randomUUID() + suffix + ".tmp";
		return new DbIndexOutput(this, tmpName);
	}

	@Override
	public void sync(Collection<String> names) throws IOException {

	}

	@Override
	public void syncMetaData() throws IOException {

	}

	@Override
	public void rename(String source, String dest) throws IOException {
		IndexFile sourcefile = getFile(source);
		IndexFile targetFile = getFile(dest);
		if (targetFile != null && !targetFile.getRawRecord().isNewRecord() && targetFile.getId() > 0){
			targetFile.destroy();
		}
		sourcefile.setName(dest);
		sourcefile.save();
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		IndexFile e = getFile(name);
		if (e == null) {
			throw new NoSuchFileException(name);
		} else {
			return new DbIndexInput(e);
		}
	}

	@Override
	public void close() throws IOException {

	}

	private List<IndexFile> getFiles() {
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
		for (IndexFile file: getFiles()){
			if (StringUtil.equals(file.getName(),name)){
				return file;
			}
		}
		return null;
	}

	private DirectoryReader reader = null;
	public DirectoryReader getReader() {
		synchronized (this){
			try {
				if (reader == null){
					reader  = DirectoryReader.open(this);
				}
			}catch (Exception ex){
				throw new RuntimeException(ex);
			}
		}
		return reader;
	}

	private IndexSearcher searcher = null;
	public IndexSearcher getIndexSearcher() throws IOException{
		synchronized (this) {
			if (!getReader().isCurrent()) {
				reader = DirectoryReader.openIfChanged(getReader());
				searcher = new IndexSearcher(reader);
			}
			if (searcher == null){
				searcher = new IndexSearcher(getReader());
			}
		}
		return  searcher;
	}
}
