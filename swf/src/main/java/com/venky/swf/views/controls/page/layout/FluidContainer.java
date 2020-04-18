package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;

import java.util.ArrayList;
import java.util.List;

public class FluidContainer extends Div{

	private static final long serialVersionUID = 178277148558082469L;
	public FluidContainer(){
		super();
		addClass("container-fluid");
	}
	
	public Row createRow(){ 
		Row row = new Row();
		addControl(row);
		return row;
	}
	public Row createHeader(){
		Row row = new Row();
		addControl(row);
		return row;
	}

	public static class Row extends Div { 
		private static final long serialVersionUID = 4595722539831680270L;
		public Row(){
			super();
		}

		public Column createColumn(){
			return createColumn(-1,-1);
		}
		public Column createColumn(int offset, int width){
			Column c = new Column();
			if (offset >= 0) {
				c.addClass("offset-" + offset);
			}
			if (width >=0 ) {
				c.addClass("col-sm-" + width);
			}
			addControl(c);
			return c;
		}

		public Column getLastColumn(){
			List<_IControl> controls = getContainedControls();
			if (controls.isEmpty()){
				return createColumn();
			}else {
				return (Column)controls.get(controls.size()-1);
			}
		}
	}

	public static class Column extends Div { 
		private static final long serialVersionUID = 8104605293906113933L;
		public Column(){
			super();
		}
	}

	public void removeColumn(int index) {
		List<Row> rows = new ArrayList<Row>();
		hunt(this, Row.class,rows);
		for (Row row: rows){
			row.removeContainedControlAt(index);
		}
	}

	
}
