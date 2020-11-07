package com.venky.swf.integration;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
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
            InputStream is = null;
            try {
                    is = path.getInputStream();
                    return reader.read(is);
            }catch(IOException ex){
                    throw new RuntimeException(ex);
            }finally {
                if (is != null){
                    try {
                        is.close();
                    }catch(IOException ex){
                        throw new RuntimeException(ex);
                    }
                }
            }
	}
	
	public View createResponse(Path path, List<M> models){
		return createResponse(path,models, null);
	}
	public View createResponse(Path path, List<M> m, List<String> includeFields) {
		return createResponse(path, m, includeFields, new HashSet<>(), new HashMap<>());
	}
	public View createResponse(Path path, List<M> m, List<String> includeFields, Set<Class<? extends Model>> ignoreParents, Map<Class<? extends Model>, List<String>> templateFields) {
		return createResponse(path, m, includeFields, ignoreParents,
				new Cache<Class<? extends Model>, List<Class<? extends Model>>>() {
					@Override
					protected List<Class<? extends Model>> getValue(Class<? extends Model> aClass) {
						return new ArrayList<>();
					}
				}
				, templateFields);
	}
	public View createResponse(Path path, List<M> m, List<String> includeFields,
							   Set<Class<? extends Model>> ignoreParents,
							   Map<Class<? extends Model>,List<Class<?extends Model>>> considerChildren,
							   Map<Class<? extends Model>, List<String>> templateFields) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writer.write(m, baos, includeFields,ignoreParents,considerChildren,templateFields);
			return new BytesView(path, baos.toByteArray());
		}catch (IOException ex){ 
			throw new RuntimeException(ex);
		}
	}
	public View createResponse(Path path, M m){
		return createResponse(path, m,null);
	}
	public View createResponse(Path path, M m, List<String> includeFields) {
		return createResponse(path, m,includeFields,new HashSet<>(), new HashMap<>());
	}
	public View createResponse(Path path, M m, List<String> includeFields,  Set<Class<? extends Model>> ignoreParents, Map<Class<? extends Model>, List<String>> templateFields) {
		return createResponse(path,m,true, includeFields, ignoreParents,templateFields);
	}
	public View createResponse(Path path, M m, boolean rootElementRequiresName,
							   List<String> includeFields,
							   Set<Class<? extends Model>> ignoreParents,
							   Map<Class<? extends Model>,List<String>> templateFields) {


		final Map<Class<? extends Model>,List<Class <? extends Model>>> map = new Cache<Class<? extends Model>,List<Class <? extends Model>>>(0,0){

			@Override
			protected List<Class<? extends Model>> getValue(Class<? extends Model> aClass) {
				return new SequenceSet<>();
			}
		};

		List<Class<? extends Model>> childModels = modelReflector.getChildModels();
		Set<String> childModelNames = new HashSet<>();
		childModels.forEach(cm->childModelNames.add(cm.getSimpleName()));

		templateFields.keySet().forEach(modelClass->{
			if (childModelNames.contains(modelClass.getSimpleName())){
				//A First level child included in templates.
				map.get(modelReflector.getModelClass()).add(modelClass);
			}
		});

		return createResponse(path, m, rootElementRequiresName, includeFields, ignoreParents,map,templateFields);
	}
	public View createResponse(Path path, M m, boolean rootElementRequiresName,
							   List<String> includeFields,
							   Set<Class<? extends Model>> ignoreParents,
							   Map<Class<? extends Model>,List<Class <? extends Model>>> considerChildren,
							   Map<Class<? extends Model>,List<String>> templateFields) {

		FormatHelper<T> helper = FormatHelper.instance(getMimeType(),modelReflector.getModelClass().getSimpleName(),false); ;
		T element = helper.getRoot();
		T elementAttribute = helper.getElementAttribute(modelReflector.getModelClass().getSimpleName());
		if (elementAttribute == null) {
			elementAttribute = element;
		}else if (!rootElementRequiresName){
			helper.removeElementAttribute(modelReflector.getModelClass().getSimpleName());
			elementAttribute = element;
		}

		T elementAttributeToWrite = elementAttribute;
		TimerUtils.time(cat,"Write Response" , ()-> {
			writer.write(m, elementAttributeToWrite , includeFields, ignoreParents, considerChildren,templateFields);
			return true;
		});
		return TimerUtils.time(cat, "Returning Bytes View" , () -> new BytesView(path, helper.toString().getBytes()));
	}
	private transient  final SWFLogger cat = Config.instance().getLogger(getClass().getName());

	
	public void writeResponse(List<M> m, OutputStream os){
		writeResponse(m, os,null);
	}
	public void writeResponse(List<M> m, OutputStream os,List<String> includeFields){
		try {
			writer.write(m, os, includeFields);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public View createStatusResponse(Path path, Throwable th){
		return createStatusResponse(path,th,"");
	}
	public View createStatusResponse(Path path, Throwable th, String message){
		IntegrationAdaptor<SWFHttpResponse,T> respAdaptor = IntegrationAdaptor.instance(SWFHttpResponse.class, getFormatClass());
		SWFHttpResponse response = Database.getTable(SWFHttpResponse.class).newRecord();
		response.setMessage(message);
		if (th == null){
			response.setStatus("OK");
			return respAdaptor.createResponse(path,response,Arrays.asList("STATUS","MESSAGE"));
		}else {
			response.setStatus("FAILED");
			if (!ObjectUtil.isVoid(th.getMessage())){
				response.setError(th.getMessage());
			}else {
				th = ExceptionUtil.getRootCause(th);
				response.setError(th.getClass().getSimpleName());
			}
			return respAdaptor.createResponse(path,response,Arrays.asList(new String[]{"STATUS","ERROR","MESSAGE"}));
		}
	}
}
