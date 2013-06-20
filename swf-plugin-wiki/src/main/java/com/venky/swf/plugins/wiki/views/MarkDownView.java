package com.venky.swf.plugins.wiki.views;

import org.pegdown.PegDownProcessor;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.wiki.db.model.Page;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.model.ModelListView;

public class MarkDownView extends HtmlView{
	Page page; 
	public MarkDownView(_IPath path,Page page) {
		super(path);
		this.page = page;
	}


	@Override
	protected void createBody(_IControl b) {
    	Table container = new Table();
    	container.addClass("hfill");
    	b.addControl(container);
    	
    	Row header = container.createHeader();
    	Column headerColumn = header.createColumn(2);
    	headerColumn.addControl(new Label(page.getTitle()));

    	Row searchFormRow = container.createRow();
		Column searchFormCell = searchFormRow.createColumn();
		searchFormCell.addControl(createSearchForm(page));
		searchFormRow.createColumn();
    	
    	
    	Row rowContainingDiv = container.createRow();
    	Column columnContainingDiv = rowContainingDiv.createColumn(2);

		
		Div markdown = new Div();
		markdown.addClass("markdown");
		columnContainingDiv.addControl(markdown);
		PegDownProcessor p = new PegDownProcessor();
		String html = p.markdownToHtml(StringUtil.read(page.getBody()));
		markdown.setText(html);
		
	}

	private Control createSearchForm(Page page){
		return ModelListView.createSearchForm(getPath());
    }
    
    private SequenceSet<HotLink> links = null; 

	@Override
	public SequenceSet<HotLink> getHotLinks(){
		if (links == null){
			links = super.getHotLinks();
			if (getPath().canAccessControllerAction("edit",String.valueOf(page.getId()))){
				HotLink edit = new HotLink();
				edit.setUrl(getPath().controllerPath()+"/edit/"+page.getId());
				edit.addControl(new Image("/resources/images/edit.png","Edit Page"));
            	links.add(edit);
			}
			
			if (getPath().canAccessControllerAction("blank",String.valueOf(page.getId()))){
            	HotLink create = new HotLink();
                create.setUrl(getPath().controllerPath()+"/blank");
                create.addControl(new Image("/resources/images/blank.png","New Page"));
            	links.add(create);
			}
			
		}
		return links;
	}
}
