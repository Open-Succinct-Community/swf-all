package com.venky.swf.plugins.lucene.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;

import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.plugins.lucene.views.model.IndexedModelListView;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.TextBox;

public class IndexedModelController<M extends Model>  extends ModelController<M>{

	public IndexedModelController(Path path) {
		super(path);
	}

	protected HtmlView createListView(List<M> records){
		return new IndexedModelListView<M>(getPath(), getModelClass(), 
				getIncludedFields(), records, createSearchForm());
	}
	
    protected Control createSearchForm(){
		Table table = new Table();
		Row row = table.createRow();
		TextBox search = new TextBox();
		search.setName("q");
		row.createColumn().addControl(search);
		row.createColumn().addControl(new Submit("Search"));
		
		Form searchForm = new Form();
		searchForm.setAction(getPath().controllerPath(),"search");
		searchForm.setMethod(SubmitMethod.GET);
		
		searchForm.addControl(table);
		return searchForm;
    }
    
    @Override
    public View index(){
    	return search();
    }

	public View search(){
		Map<String,Object> formData = getFormFields(getPath().getRequest());
		if (!formData.isEmpty()){
			rewriteQuery(formData);
			return index(search(formData));
		}else {
			return index(new ArrayList<M>());
		}
	}
	
	protected List<M> search(Map<String,Object> formData){
		LuceneIndexer indexer = LuceneIndexer.instance(getModelClass());
		String strQuery = StringUtil.valueOf(formData.get("q"));
		
		Query q = indexer.constructQuery(strQuery);
		List<Integer> ids = indexer.findIds(q, 100);
		Select sel = new Select().from(getModelClass()).where(new Expression("ID",Operator.IN,ids.toArray()));
		return sel.execute(getModelClass());
	}

	protected void rewriteQuery(Map<String,Object> formData){
		
	}
	
}
