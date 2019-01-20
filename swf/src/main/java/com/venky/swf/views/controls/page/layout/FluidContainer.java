package com.venky.swf.views.controls.page.layout;

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

	public static class Row extends Div { 
		private static final long serialVersionUID = 4595722539831680270L;
		public Row(){
			super();
		}

		public Column createColumn(int offset, int width){
			Column c = new Column();
			c.addClass("offset-" + offset);
			c.addClass("col-sm-" + width);
			addControl(c);
			return c;
		}
	}

	public static class Column extends Div { 
		private static final long serialVersionUID = 8104605293906113933L;
		public Column(){
			super();
		}
	}
	
	
	
}
