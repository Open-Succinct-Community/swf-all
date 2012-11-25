package com.venky.swf.integration;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.Response;
import com.venky.swf.db.model.io.AbstractModelReader;
import com.venky.swf.db.model.io.AbstractModelWriter;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

public class IntegrationAdaptor<M extends Model,T> {
	
	AbstractModelReader<M,T> reader ; 
	AbstractModelWriter<M,T> writer ;  
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
		writer.write(m, element, getFields(includeFields));
		return new BytesView(path, element.toString().getBytes());
	}
	
	private List<String> getFields(List<String> includeFields) {
		List<String> fields = includeFields;
		if (fields == null){
			fields = modelReflector.getFields();
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
