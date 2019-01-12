/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.model.ModelAwareness;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.FluidContainer.Row;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidTable;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.Span;
import com.venky.swf.views.controls.page.layout.Tabs;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelEditView<M extends Model> extends AbstractModelView<M> {
    private int numFieldsPerRow = 2;
    private M record; 
    public ModelEditView(Path path, String[] includeFields,M record){
    	this(path,includeFields,record,"save");
    }
    
    public ModelEditView(Path path, String[] includeFields,M record,String formAction){
		super(path,includeFields);
		this.record = record;
		this.formAction = formAction;
	}
    private String formAction = null ;    
    

	public M getRecord() {
		return record;
	}

	public int getNumFieldsPerRow() {
        return numFieldsPerRow;
    }

    public void setNumFieldsPerRow(int numFieldsPerRow) {
        this.numFieldsPerRow = numFieldsPerRow;
    }

    public int getNumColumnsPerRow(){ 
        return 2 * getNumFieldsPerRow();
    }

    protected String getFormAction(){ 
    	if (getPath().canAccessControllerAction(formAction) || (getRecord().getRawRecord().isNewRecord() && getPath().canAccessControllerAction(formAction, null))){
    		return formAction;
    	}else {
    		return "back";
    	}
    }

    protected String getDetailTabName(){
		return getModelAwareness().getReflector().getModelClass().getSimpleName() + " Information";
	}
    
    @Override
    protected void createBody(_IControl body) {
    	ModelReflector<M> reflector = getModelAwareness().getReflector();

    	Tabs multiTab = new Tabs();
		String selectedTab = StringUtil.valueOf(getPath().getFormFields().get("_select_tab"));
		if (ObjectUtil.isVoid(selectedTab)){
			selectedTab = getDetailTabName();
		}
		body.addControl(multiTab);

    	Form form =  new Form();
    	
    	final int NUM_COLUMNS_PER_ROW = 2; 
    	FluidTable table = new FluidTable(NUM_COLUMNS_PER_ROW, form);
    	multiTab.addSection(table,getDetailTabName(),StringUtil.equals(getDetailTabName(), selectedTab));
    	
    	addHotLinks(table, 0, getTabLinks(), new SequenceSet<HotLink>());
    	String action = StringEscapeUtils.escapeHtml4(getPath().controllerPath());
    	Config.instance().getLogger(getClass().getName()).fine("action:" + action);
    	
        form.setAction(action, getFormAction());
        form.setMethod(SubmitMethod.POST);
        
        Iterator<String> field = getIncludedFields().iterator();
        List<Control> hiddenFields = new ArrayList<Control>();
        
        TextBox hiddenHashField = new TextBox();
        hiddenHashField.setVisible(false);
        hiddenHashField.setName("_FORM_DIGEST");
        hiddenFields.add(hiddenHashField);
        
        while (field.hasNext()){
            String fieldName = field.next();
            
            Control fieldData = getModelAwareness().getInputControl(fieldName,fieldName, record, this);
            if (!fieldData.isEnabled() && !fieldData.isVisible()){
            	continue;
            }
            
            if (reflector.isFieldVisible(fieldName)){
                boolean forceNewRow = false;
            	boolean fieldTooLong = getModelAwareness().getReflector().isFieldDisplayLongForTextBox(fieldName);
            	if (fieldTooLong || fieldData instanceof FileTextBox){
            		forceNewRow = true;
            	}

            	FluidTable tmp = new FluidTable(12);
                Div fg = new Div();
                fg.addClass("form-group");
                
                Label fieldLabel = new Label(getModelAwareness().getFieldLiteral(fieldName));
                fieldLabel.addClass("control-label");

            	tmp.addControl(fieldLabel, forceNewRow, 0, forceNewRow ? 2 : 4);
            	Control fieldControl = fieldData; 
            	if (fieldControl instanceof FileTextBox){
            		fieldControl = ((FileTextBox)fieldData).getStylishVersion();
            	}
            	
            	if (fieldTooLong){
                    tmp.addControl(fieldControl,true, 0,  12);
            	}else {
            		tmp.addControl(fieldControl,false,0,forceNewRow? 4 : 8);
            	}
            	for (_IControl c : tmp.getContainedControls()){
                	fg.addControl(c);
            	}
            	
            	table.addControl(fg,forceNewRow,0,forceNewRow? NUM_COLUMNS_PER_ROW :1);
            	if (fieldData instanceof FileTextBox){
                	form.setProperty("enctype","multipart/form-data");
                	FileTextBox ftb = (FileTextBox)fieldData;
                	if (reflector.getContentSize(record, fieldName) != 0 && !record.getRawRecord().isNewRecord()){
                        Column column = table.addControl(ftb.getStreamLink(), true, 0, NUM_COLUMNS_PER_ROW);
                    	column.addClass("text-center");
                    }
                }
            }else {
                hiddenFields.add(fieldData);
            }
        }
        
        Row fg = new Row();
        fg.addClass("form-group");
        fg.setVisible(false);
        for (Control hiddenField: hiddenFields){
            fg.addControl(hiddenField);
        }
        table.addControl(fg);
        Column column = table.addControl(new Div(), true, 0, 4); // Blank row.
        column.addClass("empty-row");
        
        if (getRecord().getRawRecord().isNewRecord()) {
            Submit sbm = new Submit("Save & More");
            sbm.setToolTip("Done with this but more to go");
            sbm.setName("_SUBMIT_MORE");
            column = table.addControl(sbm,true,0,1);
            column.addClass("text-right");
            sbm = new Submit("Done");
        	sbm.setToolTip("Done with all");
	        sbm.setName("_SUBMIT_NO_MORE");
	        column = table.addControl(sbm,false,0,1);
	        column.addClass("text-left");
        }else {
            String label = "Save";
            if (getFormAction().equals("back")){
            	label = "Close";
            }
            
        	Submit sbm = new Submit(label);
	        sbm.setName("_SUBMIT_NO_MORE");
	        column = table.addControl(sbm,true,0,2);
	        column.addClass("text-center");
	    	List<Class<? extends Model>> childModels = reflector.getChildModels(true, true);

	    	
    		for (Class<? extends Model> childClass: childModels){
				Path childPath = new Path(getPath().getTarget()+"/"+ LowerCaseStringCache.instance().get(Database.getTable(childClass).getTableName()) + "/index");
	        	childPath.setRequest(getPath().getRequest());
	        	childPath.setResponse(getPath().getResponse());
	        	childPath.setSession(getPath().getSession());
	        	ModelAwareness childModelAwareness = new ModelAwareness(childPath, null);
	        	if (childPath.canAccessControllerAction()){
	            	FluidContainer tab = new FluidContainer();
	            	if (!getModelAwareness().getReflector().isVirtual() && !childModelAwareness.getReflector().isVirtual() ) {
						addChildModelToTab(childPath,tab);
					}else {
	            		for (Method childGetter : getModelAwareness().getReflector().getChildGetters()){
	            			if (getModelAwareness().getReflector().getChildModelClass(childGetter).equals(childClass)){
								try {
									addChildModelToTab(childPath,tab,childClass,childGetter);
								} catch (Exception e) {
									Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Could not add model " + childClass.getSimpleName() , e);
								}
							}
						}
					}
	        		String tabName = childModelAwareness.getLiteral(childClass.getSimpleName());
	        		multiTab.addSection(tab,tabName,StringUtil.equals(selectedTab,tabName));
	        	}
	        }    

        }
        hiddenHashField.setValue(Encryptor.encrypt(getModelAwareness().getHashFieldValue().toString()));
    }

	private <T extends  Model> void addChildModelToTab(Path childPath, Div tab, Class<T> childClass, Method childGetter) {
		try {
			List<T> children = (List<T>)childGetter.invoke(getRecord());
			new ModelListView<T>(childPath,null,children, true).createBody(tab);
		} catch (Exception ex){
            Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Could not add model " + childClass.getSimpleName() , ex);
		}

	}

	protected final void addChildModelToTab(Path childPath,Div tab){
    	SequenceSet<HotLink> excludeLinks = new SequenceSet<HotLink>();
    	excludeLinks.addAll(getHotLinks());
    	//Exclude back link on included child as it is a redundant link to the record in focus currently.
    	HotLink childBack = new HotLink(childPath.controllerPath() + "/back");
    	excludeLinks.add(childBack);

    	DashboardView view =  (DashboardView)childPath.invoke();
    	view.createBody(tab,true,false,excludeLinks);
    }
    private SequenceSet<HotLink> links = null;
    
    
    public SequenceSet<HotLink> getTabLinks(){
    	if (links == null){
    		links = new SequenceSet<HotLink>();
    		
            if (!getFormAction().equals("back")){
                HotLink sbm = new HotLink("#save");
                sbm.addControl(new Glyphicon("glyphicon-floppy-disk","Submit"));
                sbm.setName("_SUBMIT_NO_MORE");
    	        links.add(sbm);
            }

	        if (getRecord().getRawRecord().isNewRecord()) {
                HotLink sbm = new HotLink("#save_and_move_to_next");
                sbm.addControl(new Glyphicon("glyphicon-duplicate","Save & use as template for next."));
                sbm.setName("_SUBMIT_MORE");
                links.add(sbm);
            }
            

    		for (Method m : getModelAwareness().getSingleRecordActions()){
            	String actionName = m.getName();
            	if (actionName.equals("destroy") || actionName.equals(getPath().action())){
            		continue;
            	}
            	Link actionLink = getModelAwareness().createSingleRecordActionLink(m, getRecord());
            	if (actionLink != null) {
            		links.add(new HotLink(actionLink));
            	}
            }


    	}
    	return links;
    }
    
    
}
