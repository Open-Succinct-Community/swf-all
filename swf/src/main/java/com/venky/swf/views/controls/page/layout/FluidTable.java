package com.venky.swf.views.controls.page.layout;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;

public class FluidTable extends FluidContainer{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1018558947954429757L;
	private int numColumnsLayout = 1; 
	private int width = 12/numColumnsLayout;
	private void setNumColumnsLayout(int maxColumnsPerRow){
		if (maxColumnsPerRow < 1 || maxColumnsPerRow > 12){
			throw new RuntimeException("maxColumnsPerRow out of bound [1,12]");
		}
		for (int i = Math.min(12,maxColumnsPerRow) ; i > 1 ; i -- ){
			if (12 % i == 0){
				numColumnsLayout = i;
				width = 12/numColumnsLayout;
				break;
			}
		}
		
	}
	public FluidTable(int maxColumnsPerRow){
		this(maxColumnsPerRow,null);
	}
	public FluidTable(int maxColumnsPerRow, _IControl layoutControl){
		super();
		setNumColumnsLayout(maxColumnsPerRow);
		if (layoutControl != null ){
			this.layoutControl = layoutControl;
			super.addControl(layoutControl);
		}else {
			this.layoutControl = null;
		}
		
	}
	
	_IControl layoutControl ;
	
	int numColumnsOccupied = 0;
	Row currentRow = null;
	@Override
	public void addControl(_IControl control){
		addControl(control,false,0,1);
	}
	
	public Column addControl(_IControl control, boolean forceNewRow, int colSpanOffset, int colSpan){
		
		if (forceNewRow) {
			numColumnsOccupied= 0;
		}
		
		int controlIndexInRowBeingAdded = (numColumnsOccupied % numColumnsLayout);
		if (control.isVisible() || currentRow == null) {
			if (controlIndexInRowBeingAdded == 0){
				currentRow = new Row();
				if (layoutControl != null){
					layoutControl.addControl(currentRow);
				}else{ 
					super.addControl(currentRow); //Prevent Recurrsion.
				}
			}
		}
		
		Column column = null;
		if (control.isVisible()) {
			column = currentRow.createColumn(colSpanOffset * width, width * colSpan);
		}else {
			List<Column> columns = new ArrayList<Column>();
			Control.hunt(currentRow, Column.class, columns);
			if (columns.isEmpty()){
				column = currentRow.createColumn(colSpanOffset * width, width * 1);
			}else {
				column = columns.get(columns.size() - 1);
			}
		}
		column.addControl(control);
		
		numColumnsOccupied = (numColumnsOccupied + colSpanOffset + colSpan)%numColumnsLayout; 
		return column;
	}
	
	
}
