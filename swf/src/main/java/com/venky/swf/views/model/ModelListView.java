/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.util.ArrayList;
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
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.FluidContainer.Row;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.Panel;
import com.venky.swf.views.controls.page.layout.Panel.PanelHeading;
import com.venky.swf.views.controls.page.layout.Span;
import com.venky.swf.views.controls.page.layout.headings.H;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelListView<M extends Model> extends AbstractModelView<M> {

	private FluidContainer container ;
	private PanelHeading headingPanel ;
	private Panel contentPanel;
	public PanelHeading getHeadingPanel() {
		return headingPanel;
	}
	public ModelListView(Path path, String[] includeFields, List<M> records, boolean isCompleteList) {
		super(path, includeFields);
		
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
        container = new FluidContainer();
    	Row containerRow = container.createRow();
    	
    	contentPanel = new Panel();
    	containerRow.createColumn(0, 12).addControl(contentPanel);
    	
    	headingPanel = contentPanel.createPanelHeading(); 
    	headingPanel.setTitle(getModelAwareness().getLiteral(getModelAwareness().getReflector().getModelClass().getSimpleName()));
    	
    	if (!isCompleteList){
        	List<H> hunted = new ArrayList<H>();
        	Control.hunt(headingPanel, H.class, hunted);

        	Span alertIcon = new Span();
    		alertIcon.addClass("glyphicon glyphicon-alert");
    		alertIcon.setToolTip("Listing is possibly incomplete, Refine your search to find what you need");
    		
    		hunted.get(0).addControl(alertIcon);
    	}
    	
    	
    	boolean indexedModel = !getModelAwareness().getReflector().getIndexedFieldGetters().isEmpty();

    	if (indexedModel){
    		createSearchForm(getPath(),headingPanel);
    	}
    	
    	Row tableCellRow = new Row();
    	contentPanel.addControl(tableCellRow);
    	Column tableCell = tableCellRow.createColumn(0,12);
    	
    	ModelListTable<M> modelListTable = createModelListTable(path);
    	tableCell.addControl(modelListTable);
    	modelListTable.addRecords(records);

    }
	@Override
	public boolean isFieldVisible(String fieldName) {
		return getIncludedFields().contains(fieldName);
	}
	protected ModelListTable<M> createModelListTable(Path path){
		return new ModelListTable<M>(path,getModelAwareness(),this);
	}
    
    static void createSearchForm(_IPath path, Div container){
    	Row row = new Row();
    	container.addControl(row);
    	Column col = row.createColumn(0, 12);
    	
    	Form searchForm = new Form();
		searchForm.setAction(StringEscapeUtils.escapeHtml4(path.controllerPath()),"search");
		searchForm.setMethod(SubmitMethod.GET);
		col.addControl(searchForm);

		
		Row contentRow = new Row();
		searchForm.addControl(contentRow);

		col = contentRow.createColumn(0, 6);
		TextBox search = new TextBox();
		search.setName("q");
		search.setValue(path.getFormFields().get("q"));
		search.setWaterMark("Refine your search here..."); 
		search.addClass("form-control");
		col.addControl(search);
		
		col = contentRow.createColumn(0, 2);
		col.addControl(new Submit("Search"));

		
    }
    
    
    private SequenceSet<HotLink> links = null;
    @Override
    public SequenceSet<HotLink>  getHotLinks(){
    	if (links == null ){
    		links = super.getHotLinks();
    		if (getPath().canAccessControllerAction("blank") && getPath().canAccessControllerAction("save")){
            	HotLink create = new HotLink();
                create.setUrl(getPath().controllerPath()+"/blank");
                create.addControl(new Glyphicon("glyphicon-plus","New"));
            	links.add(create);
        	}
        	if (getPath().canAccessControllerAction("importxls") && getPath().canAccessControllerAction("save")){
        		HotLink importxls = new HotLink();
        		importxls.setUrl(getPath().controllerPath()+"/importxls");
        		importxls.addControl(new Glyphicon("glyphicon-cloud-upload","Upload XLS data"));
    			links.add(importxls);
        	}
        	
        	if (getPath().canAccessControllerAction("exportxls")){
        		HotLink exportxls = new HotLink();
        		exportxls.setUrl(getPath().controllerPath()+"/exportxls");
        		exportxls.addControl(new Glyphicon("glyphicon-cloud-download","Download data as xls"));
    			links.add(exportxls);
        	}
    	}
    	return links;
    }
    
    @Override
    protected void createBody(_IControl b) {
    	b.addControl(container);
    }

}
