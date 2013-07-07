/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.model.ModelListTable;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelListView<M extends Model> extends AbstractModelView<M> {

    private List<M> records;
    private ModelListTable<M> modelListTable;
	public ModelListView(Path path, String[] includeFields, List<M> records) {
		super(path, includeFields);
		this.records = records;
		
		ModelReflector<M> reflector = getModelAwareness().getReflector();
        if (includeFields == null){
        	Iterator<String> fi = getIncludedFields().iterator(); 
        	while (fi.hasNext()){
        		String field = fi.next();
        		if (reflector.isHouseKeepingField(field) || !reflector.isFieldVisible(field)) {
	        		fi.remove();
	        	}
	        }
        }
        
    	this.modelListTable = createModelListTable(path);
        
    }
	protected ModelListTable<M> createModelListTable(Path path){
		return new ModelListTable<M>(path,getIncludedFields().toArray(new String[]{}),this);
	}
    
    public static Control createSearchForm(_IPath path){
    	com.venky.swf.views.controls.page.layout.Table table = new com.venky.swf.views.controls.page.layout.Table();
    	table.addClass("search");
    	
		Row row = table.createRow();
		TextBox search = new TextBox();
		search.setName("q");
		search.setValue(path.getFormFields().get("q"));

		row.createColumn().addControl(search);

		row.createColumn().addControl(new Submit("Search"));
		
		Form searchForm = new Form();
		searchForm.setAction(StringEscapeUtils.escapeHtml4(path.controllerPath()),"search");
		searchForm.setMethod(SubmitMethod.GET);
		
		searchForm.addControl(table);
		return searchForm;
    }
    
    
    private SequenceSet<HotLink> links = null;
    @Override
    public SequenceSet<HotLink>  getHotLinks(){
    	if (links == null ){
    		links = super.getHotLinks();
    		if (getPath().canAccessControllerAction("blank") && getPath().canAccessControllerAction("save")){
            	HotLink create = new HotLink();
                create.setUrl(getPath().controllerPath()+"/blank");
                create.addControl(new Image("/resources/images/blank.png","New"));
            	links.add(create);
        	}
        	if (getPath().canAccessControllerAction("importxls") && getPath().canAccessControllerAction("save")){
        		HotLink importxls = new HotLink();
        		importxls.setUrl(getPath().controllerPath()+"/importxls");
        		importxls.addControl(new Image("/resources/images/importxls.png","Import"));
    			links.add(importxls);
        	}
        	
        	if (getPath().canAccessControllerAction("exportxls")){
        		HotLink exportxls = new HotLink();
        		exportxls.setUrl(getPath().controllerPath()+"/exportxls");
        		exportxls.addControl(new Image("/resources/images/exportxls.png","Export"));
    			links.add(exportxls);
        	}
    	}
    	return links;
    }
    
    @Override
    protected void createBody(_IControl b) {
    	
    	Table container = new Table();
    	container.addClass("hfill");
    	b.addControl(container);
    	
    	Row header = container.createHeader();
    	Column headerColumn = header.createColumn(2);
    	headerColumn.addControl(new Label(getModelAwareness().getReflector().getModelClass().getSimpleName()));
    	boolean indexedModel = !getModelAwareness().getReflector().getIndexedFieldGetters().isEmpty();

    	if (indexedModel){
    		Row searchFormRow = container.createRow();
    		Column searchFormCell = searchFormRow.createColumn();
    		searchFormCell.addControl(createSearchForm(getPath()));
    		searchFormRow.createColumn();
    	}
    	
    	
    	
    	Row rowContainingTable = container.createRow();
    	Column columnContainingTable = rowContainingTable.createColumn(2);
    	
    	columnContainingTable.addControl(modelListTable);
    	modelListTable.addRecords(records);
    }

}
