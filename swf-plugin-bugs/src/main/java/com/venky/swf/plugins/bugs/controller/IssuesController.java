package com.venky.swf.plugins.bugs.controller;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.ModelEditView;
import com.venky.swf.views.model.ModelListView;

public class IssuesController extends ModelController<Issue>{

	public IssuesController(Path path) {
		super(path);
	}
	protected HtmlView createListView(List<Issue> records){
		return new ModelListView<Issue>(getPath(), Issue.class, new String[]{"TITLE","PRIORITY","STATUS","ASSIGNED_TO_ID","RESOLUTION"}, records);
    }
	
	protected ModelEditView<Issue> createBlankView(Path path , Issue record){
		ModelEditView<Issue> mev = super.createBlankView(path, record);
		mev.getIncludedFields().removeAll(Arrays.asList("STATUS","RESOLUTION"));
		Logger.getLogger(IssuesController.class.getName()).info(mev.getIncludedFields().toString());
		
		return mev;
	}
	
	@SingleRecordAction(tooltip="Yank")
	public View yank(int id){
		Issue issue = Database.getTable(Issue.class).get(id);
		issue.yank();
		return afterPersistDBView(issue);
	}
	
}
