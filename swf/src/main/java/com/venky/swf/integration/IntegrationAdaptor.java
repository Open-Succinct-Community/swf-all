package com.venky.swf.integration;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.Response;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

public class IntegrationAdaptor<M extends Model,T> {
	
	ModelReader<M,T> reader ; 
	ModelWriter<M,T> writer ;  
	ModelReflector<M> modelReflector;  
	Class<T> formatClass ;
	private IntegrationAdaptor(Class<M> modelClass,Class<T> formatClass){
		this.formatClass = formatClass;
		this.reader = ModelIOFactory.getReader(modelClass, formatClass);
		this.writer = ModelIOFactory.getWriter(modelClass, formatClass); 
		this.modelReflector = ModelReflector.instance(modelClass); 
	}
	public Class<T> getFormatClass(){
		return formatClass;
	}
	
	public MimeType getMimeType(){
		return FormatHelper.getMimeType(getFormatClass());
	}
	
	public static <M extends Model,T> IntegrationAdaptor<M, T> instance(Class<M> modelClass,Class<T> formatType){
		IntegrationAdaptor<M, T> adaptor = null; 
		MimeType mimeType = FormatHelper.getMimeType(formatType);
		switch (mimeType){
			case APPLICATION_JSON:
			case APPLICATION_XML:
				adaptor = new IntegrationAdaptor<M, T>(modelClass, formatType);
				break;
			default:
				break;
		}
		return adaptor;
	}
	
	public List<M> readRequest(Path path){
		try {
			InputStream is = path.getRequest().getInputStream();
			is = new ByteArrayInputStream(StringUtil.readBytes(is));
			return reader.read(is);
		}catch(IOException ex){
			throw new RuntimeException(ex);
		}
	}
	
	public View createResponse(Path path, List<M> models){
		return createResponse(path,models, null);
	}
	public View createResponse(Path path, M m){
		return createResponse(path, m,null);
	}
	public View createResponse(Path path, M m, List<String> includeFields) {
		FormatHelper<T> helper = FormatHelper.instance(getMimeType(),modelReflector.getModelClass().getSimpleName(),false); 
		T element = helper.getRoot();
		writer.write(m, element , getFields(includeFields));
		return new BytesView(path, element.toString().getBytes());
	}
	
	private List<String> getFields(List<String> includeFields) {
		List<String> fields = includeFields;
		if (fields == null){
			fields = modelReflector.getFields();
			Iterator<String> fi = fields.iterator();
			while (fi.hasNext()){
				String field = fi.next();
				if (modelReflector.isFieldHidden(field) && !"ID".equalsIgnoreCase(field)){
					fi.remove();
				}
			}
		}
		return fields;
	}
	
	public View createResponse(Path path, List<M> m, List<String> includeFields) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writer.write(m, baos, getFields(includeFields));
			return new BytesView(path, baos.toByteArray());
		}catch (IOException ex){ 
			throw new RuntimeException(ex);
		}
	}
	
	public void writeResponse(List<M> m, OutputStream os){
		writeResponse(m, os,null);
	}
	public void writeResponse(List<M> m, OutputStream os,List<String> includeFields){
		try {
			writer.write(m, os, getFields(includeFields));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public View createStatusResponse(Path path, Throwable th){
		IntegrationAdaptor<Response,T> respAdaptor = IntegrationAdaptor.instance(Response.class, getFormatClass());
		Response response = Database.getTable(Response.class).newRecord();
		if (th == null){
			response.setStatus("OK");
			return respAdaptor.createResponse(path,response,Arrays.asList("STATUS"));
		}else {
			response.setStatus("FAILED");
			response.setError(th.getMessage());
			return respAdaptor.createResponse(path,response,Arrays.asList(new String[]{"STATUS","ERROR"}));
		}
	}
	
	
}
