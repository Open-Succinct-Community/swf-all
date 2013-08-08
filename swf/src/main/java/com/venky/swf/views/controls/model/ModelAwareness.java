package com.venky.swf.views.controls.model;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.UpperCaseStringCache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.ui.TOOLTIP;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.text.AutoCompleteText;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.DateBox;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.OptionCreator;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.RadioGroup;
import com.venky.swf.views.controls.page.text.Select;
import com.venky.swf.views.controls.page.text.TextArea;
import com.venky.swf.views.controls.page.text.TextBox;
import com.venky.swf.views.model.FieldUIMetaProvider;

public class ModelAwareness implements FieldUIMetaProvider{

	public ModelAwareness(Path path, String[] includeFields) {
		this.path = path;
		this.orderby = new OrderBy();
		this.includedFields.addAll(getReflector().getFields());
		if (includeFields != null && includeFields.length > 0){
			this.includedFields.retainAll(Arrays.asList(includeFields));
		}
	}
	
	private List<String> includedFields = new IgnoreCaseList(false);
	public List<String> getIncludedFields(){
		return includedFields;
	}
	
	private Path path = null;
	public Path getPath(){ 
		return path;
	}

	@SuppressWarnings("unchecked")
	public <M extends Model> ModelReflector<M> getReflector(){
		return (ModelReflector<M>) ModelReflector.instance(getPath().getModelClass());
	}
	
	/* Model Awareness */

    public String getFieldLiteral(String fieldName){
        String fieldLiteral =  getLiteral(StringUtil.camelize(fieldName));

        Method parentModelgetter = getReflector().getReferredModelGetterFor(getReflector().getFieldGetter(fieldName));
        if (parentModelgetter != null) {
            fieldLiteral = getLiteral(parentModelgetter.getName().substring("get".length())) ;
        }
        return fieldLiteral;
        
    }
    
    public String getLiteral(String camel){
    	
    	List<Integer> upper = new ArrayList<Integer>();
        byte[] bytes = camel.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b < 97 || b > 122) {
                upper.add(i);
            }
        }

        StringBuilder b = new StringBuilder(camel);
        for (int i = upper.size() - 1; i >= 0; i--) {
            Integer index = upper.get(i);
            if (index != 0)
                b.insert(index, " ");
        }
    	
        return b.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public <M extends Model> Control getInputControl(String controlName, String fieldName, M record, FieldUIMetaProvider metaprovider) {
    	if (metaprovider == null){
    		metaprovider = this;
    	}
    	ModelReflector<M> reflector = getReflector();
        Method getter = reflector.getFieldGetter(fieldName);
        Object value = null;
        try {
            value = getter.invoke(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Class<?> returnType = getter.getReturnType();
        TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(returnType).getTypeConverter();
        Control control = null;
        if (boolean.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType)) {
            CheckBox cb = new CheckBox();
            cb.setChecked(converter.toString(value));
            control = cb;
        }else if (reflector.isFieldValueLongForTextBox(fieldName)){
        	TextArea txtArea = new TextArea();
        	txtArea.setText(converter.toString(value));
        	control = txtArea;
        }else if (InputStream.class.isAssignableFrom(returnType)){
        	FileTextBox ftb = new FileTextBox();
        	String contentType = reflector.getContentType(record, fieldName);
			if (!ObjectUtil.isVoid(contentType)){
				ftb.setContentType(contentType);
			}
        	if (reflector.getContentSize(record, fieldName) != 0 && !record.getRawRecord().isNewRecord()){
        		ftb.setStreamUrl(path.controllerPath()+"/view/"+record.getId(),reflector.getContentName(record, fieldName));
        	}
        	control = ftb;
        }else {
            Method parentModelGetter = reflector.getReferredModelGetterFor(getter);
            if (parentModelGetter != null){
                control = new AutoCompleteText(reflector.getReferredModelClass(parentModelGetter),path.controllerPath()+"/autocomplete");
                if (reflector.isAnnotationPresent(getter,OnLookupSelect.class)){
                	((AutoCompleteText)control).setOnAutoCompleteSelectProcessingUrl(path.controllerPath()+"/onAutoCompleteSelect");
                }
            }else if (Date.class.isAssignableFrom(returnType)){
            	control = new DateBox(); 
            }else if (reflector.isFieldPassword(fieldName)){
                control = new PasswordText();
            }else if (reflector.isFieldEnumeration(fieldName)){
                //Select select = new Select();
                Enumeration enumeration = reflector.getAnnotation(reflector.getFieldGetter(fieldName),Enumeration.class) ;
                OptionCreator select = null;
                if (enumeration.showAs() == null || enumeration.showAs().equals(Select.class.getSimpleName())){
                	select = new Select();
                }else {
                	select = new RadioGroup();
                }
                StringTokenizer allowedValues = new StringTokenizer(enumeration.value(),",");
                
                while (allowedValues.hasMoreTokens()){
                	String nextAllowedValue = allowedValues.nextToken();
                	select.createOption(nextAllowedValue, nextAllowedValue);
                }
                control = select;
            }else{
            	control = new TextBox();
            }
            if (!ObjectUtil.isVoid(value)){
            	control.setValue(converter.toString(value));
            }
        }
        control.setName(controlName);

        if (!metaprovider.isFieldVisible(fieldName)){
        	control.setVisible(false);
        }else if (!metaprovider.isFieldEditable(fieldName)){
            Kind protectionKind = metaprovider.getFieldProtection(fieldName);
            switch(protectionKind){
            	case NON_EDITABLE:
            		control.setEnabled(true);
            		control.setReadOnly(true);
            		break;
            	default:
            		control.setEnabled(false);
                    break;
            }
        }
    
        if (control.isVisible() && control.isEnabled() && !control.isReadOnly()){
	        WATERMARK watermark = getReflector().getAnnotation(getter, WATERMARK.class);
	        if (watermark != null){
	        	control.setWaterMark(watermark.value());
	        }
	        TOOLTIP tooltip = getReflector().getAnnotation(getter, TOOLTIP.class);
	        if (tooltip != null){
	        	control.setToolTip(tooltip.value());
	        }
        }else {
        	control.setWaterMark(null);
        	control.setToolTip(null);
        }

        if (control.isEnabled() && getReflector().isFieldSettable(fieldName)){
        	if (hashFieldValue.length() > 0){
        		hashFieldValue.append(",");
        	}
        	String fieldValue = control.getValue();
            if (control instanceof CheckBox){
            	fieldValue = StringUtil.valueOf(((CheckBox)control).isChecked());
            }else if  (control instanceof TextArea){
            	fieldValue = control.getText();
            }else if (control instanceof AutoCompleteText){
            	AutoCompleteText ac = (AutoCompleteText)control;
            	TextBox hiddenField = ac.getHiddenField();
            	hashFieldValue.append(hiddenField.getName());
            	hashFieldValue.append("=");
            	hashFieldValue.append(StringUtil.valueOf(hiddenField.getValue()));
            	hashFieldValue.append(",");
            }
            
        	hashFieldValue.append(control.getName());
        	hashFieldValue.append("=");
        	hashFieldValue.append(StringUtil.valueOf(fieldValue));
        }
        
        return control;
    }

    private StringBuilder hashFieldValue = new StringBuilder();
	public StringBuilder getHashFieldValue(){
		return hashFieldValue;
	}
	
    public String getParentDescription(Method parentIdGetter, Model record){
        Method parentModelGetter = getReflector().getReferredModelGetterFor(parentIdGetter);
        
        if (parentModelGetter != null){
        	try {
	            Model parentModel = (Model)parentModelGetter.invoke(record);
	            if (parentModel != null){
	                @SuppressWarnings("unchecked")
					Class<? extends Model> parentModelClass = (Class<? extends Model>)parentModelGetter.getReturnType();
	                ModelReflector<? extends Model> parentModelReflector = ModelReflector.instance(parentModelClass);
	                String descriptionColumn = parentModelReflector.getDescriptionField();
	                Method descGetter = parentModelReflector.getFieldGetter(descriptionColumn);
	                Object descValue = descGetter.invoke(parentModel);
	                return Database.getJdbcTypeHelper().getTypeRef(descGetter.getReturnType()).getTypeConverter().toString(descValue);
	            }
        	}catch( IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
        		throw new RuntimeException(ex);
        	}
        }
        return null;
    }

    public List<Method> getSingleRecordActions(){
    	return getPath().getControllerReflector().getSingleRecordActionMethods();
    }
    
    public <M extends Model> Link createSingleRecordActionLink(Method m, M record){
    	String actionName = m.getName();
    	SingleRecordAction sra = getPath().getControllerReflector().getAnnotation(m,SingleRecordAction.class);
    	
    	if (sra == null){
    		return null;
    	}
    	
    	boolean canAccessAction = record.getId() > 0  && getPath().canAccessControllerAction(actionName,String.valueOf(record.getId()));
    	if (!canAccessAction){
    		return null;
    	}
    	Link actionLink = new Link();
    	String icon = "/resources/images/show.png" ; 
    	String tooltip = StringUtil.camelize(actionName);
    	if (sra != null) {
    		if (!ObjectUtil.isVoid(sra.icon())){
        		icon = sra.icon(); 
    		}
    		if (!ObjectUtil.isVoid(sra.tooltip())){
        		tooltip = sra.tooltip(); 
    		}
    	}
        StringBuilder sAction = new StringBuilder();
        if ("search".equals(getPath().action())){
        	sAction.append(getPath().controllerPath()).append("/").append(getPath().action()).append("/").append(StringUtil.valueOf(getPath().getFormFields().get("q")));
        }else {
        	sAction.append(getPath().controllerPath()).append("/").append(getPath().action());
        	if (!ObjectUtil.isVoid(getPath().parameter())){
        		sAction.append("/").append(getPath().parameter());
        	}
        }
    	sAction.append("/").append(getPath().controllerPathElement());
    	sAction.append("/").append(actionName).append("/").append(record.getId());
    	actionLink.setUrl(sAction.toString());

    	actionLink.addControl(new Image(icon,tooltip));

    	return actionLink;
    }

	public ControllerReflector<? extends Controller> getControllerReflector() {
		return ControllerReflector.instance(getPath().getControllerClass());
	}

	private OrderBy orderby = null ;
	
	public class OrderBy {
    	int sortDirection(){
    		if (StringUtil.equals(ascOrDesc, "ASC")){
    			return 0;
    		}else {
    			return 1;
    		}
    	}
    	String field = null;
    	String ascOrDesc = "ASC";
    	public OrderBy(){
    		String orderBy = new StringTokenizer(getReflector().getOrderBy(), ",").nextToken();
    		StringTokenizer tok = new StringTokenizer(orderBy);
    		this.field = tok.nextToken();
    		if (tok.hasMoreTokens()){
        		this.ascOrDesc = UpperCaseStringCache.instance().get(tok.nextToken());
    		}
    	}
    }
	
	public OrderBy getOrderBy(){
		return orderby;
	}

	@Override
	public boolean isFieldVisible(String fieldName) {
		return getReflector().isFieldVisible(fieldName);
	}

	@Override
	public boolean isFieldEditable(String fieldName) {
		return getReflector().isFieldEditable(fieldName);
	}

	@Override
	public Kind getFieldProtection(String fieldName) {
		return getReflector().getFieldProtection(fieldName);
	}
	


}
