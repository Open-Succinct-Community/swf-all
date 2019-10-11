package com.venky.swf.plugins.bugs.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.routing.Config;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.AbstractModelView;
import com.venky.swf.views.model.ModelListView;

import java.util.Arrays;
import java.util.List;

public class IssuesController extends ModelController<Issue>{

	public IssuesController(Path path) {
		super(path);
	}
	@Override
	protected HtmlView constructModelListView(List<Issue> records,boolean isCompleteList){
		return new ModelListView<Issue>(getPath(), new String[]{"ID","TITLE","PRIORITY","STATUS","ASSIGNED_TO_ID","RESOLUTION","CREATOR_USER_ID"}, records, isCompleteList);
    }
	
	@Override
	protected HtmlView createBlankView(Path path , Issue record, String formAction){
		HtmlView bv = super.createBlankView(path, record,formAction);
		if (bv instanceof AbstractModelView){
			AbstractModelView mev = ((AbstractModelView)bv);
			mev.getIncludedFields().removeAll(Arrays.asList("STATUS","RESOLUTION"));
			Config.instance().getLogger(IssuesController.class.getName()).info(mev.getIncludedFields().toString());
		}

		return bv;
	}
	
	@SingleRecordAction(tooltip="Yank")
	public View yank(long id){
		Issue issue = Database.getTable(Issue.class).get(id);
		issue.yank();
		return afterPersistDBView(issue);
	}
	
}
