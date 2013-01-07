package com.venky.swf.plugins.wiki.views;

import org.pegdown.PegDownProcessor;

import com.venky.core.string.StringUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.wiki.db.model.Page;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.model.ModelListView;

public class MarkDownView extends HtmlView{
	Page page; 
	public MarkDownView(_IPath path,Page page) {
		super(path);
		this.page = page;
	}


	@Override
	protected void createBody(Body b) {
		b.addControl(createSearchForm(page));			
		Div markdown = new Div();
		b.addControl(markdown);
		PegDownProcessor p = new PegDownProcessor();
		String html = p.markdownToHtml(StringUtil.read(page.getBody()));
		markdown.setText(html);
		
	}

	private Control createSearchForm(Page page){
		Control searchForm = ModelListView.createSearchForm(getPath());

		Table tableLinks = new Table();
		Row row = tableLinks.createRow();

    	Link home = new Link("/pages");
		home.setText("Home");
		row.createColumn().addControl(home);
		if (getPath().canAccessControllerAction("edit",String.valueOf(page.getId()))){
			Link edit = new Link(getPath().controllerPath()+"/edit/"+page.getId());
			edit.setText("Edit");
			row.createColumn().addControl(edit);
		}
		
		if (getPath().canAccessControllerAction("blank",String.valueOf(page.getId()))){
			Link blank = new Link(getPath().controllerPath()+"/blank");
			blank.setText("New Page");
			row.createColumn().addControl(blank);
		}
		
		searchForm.addControl(0, tableLinks);
		return searchForm;
    }
    
	
}
