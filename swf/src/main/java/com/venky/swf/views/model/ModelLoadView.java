package com.venky.swf.views.model;

import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.FileTextBox;

public class ModelLoadView<M extends Model> extends AbstractModelView<M>{

	public ModelLoadView(Path path, Class<M> modelClass, String[] includedFields) {
		super(path, modelClass, includedFields);
	}
	
    protected Control createLoadForm(){
    	Table table = new Table();
		Row row = table.createRow();
		
		FileTextBox ftb  = new FileTextBox();
		ftb.setName("datafile");
		row.createColumn().addControl(ftb);
		row.createColumn().addControl(new Submit("Load"));
		
		Form loadForm = new Form();
		loadForm.setMethod(SubmitMethod.POST);
		loadForm.setProperty("enctype","multipart/form-data");
		
		loadForm.setAction(getPath().controllerPath(),"load");
		loadForm.addControl(table);
		
		return loadForm;
    }

	@Override
	protected void createBody(Body b) {
		b.addControl(createLoadForm());
	}

}
