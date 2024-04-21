package com.venky.swf.db.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.venky.swf.db.model.Model;

public interface ModelReader<M extends Model,T> {
	public List<M> read(InputStream in,boolean saveRecursive) throws IOException;

	public List<M> read(InputStream in, String rootElementName , boolean saveRecursive) throws IOException;
	public M read(T source,boolean saveRecursive);
	public M read(T source,boolean ensureAccessibleByLoggedInUser,boolean saveRecursive);
	public M read(T source, boolean ensureAccessibleByLoggedInUser, boolean updateAttributesFromElement, boolean saveRecursive);


	public void setInvalidReferencesAllowed(boolean invalidReferencesAllowed);
	public boolean isInvalidReferencesAllowed();
}
