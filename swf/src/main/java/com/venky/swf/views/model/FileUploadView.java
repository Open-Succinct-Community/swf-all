package com.venky.swf.views.model;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.swf.path.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.FileTextBox;

public class FileUploadView extends HtmlView{

	public FileUploadView(Path path) {
		super(path);
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
		String action = StringEscapeUtils.escapeHtml4(getPath().getOriginalRequestUrl());
		loadForm.setAction(action);
		loadForm.addControl(table);
		return loadForm;
    }

	@Override
	protected void createBody(_IControl  b) {
		b.addControl(createLoadForm());
	}

}
