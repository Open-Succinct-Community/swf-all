/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls.Control;
import java.util.List;

/**
 *
 * @author venky
 */
public class Table extends Control{
    public Table(){
        super("table");
        thead = new THead();
        addControl(thead);
        tbody = new TBody();
        addControl(tbody);
    }
    
    private THead thead = null;
    private TBody tbody = null; 
    
    public Row createRow(){
        Row row = new Row(false);
        tbody.addControl(row);
        return row;
    }
    public Row createHeader(){
        Row header = new Row(true);
        thead.addControl(header);
        return header;
    }
    
    public static class Row extends Control {
        private boolean header = false;
        public Row(boolean header){
            this("tr");
            this.header = header;
        }
        protected Row(String tag){
            super(tag);
        }
        
        private int colspanConsumed = 0;
        public Column createColumn(int colspan){
            Column c = new Column(header); 
            addControl(c);
            if (colspan > 1){
                colspanConsumed += colspan;
                c.setProperty("colspan", colspan);
            }else {
                colspanConsumed ++;
            }
            
            return c;
        }

        public Column createColumn(){
            return createColumn(1);
        }
        
        public int numColumns(){
            return colspanConsumed;
        }
        
        public Column getLastColumn(){
            List<Control> controls = getContainedControls();
            if (controls.isEmpty()){
                return createColumn();
            }else {
                return (Column)controls.get(controls.size()-1);
            }
        }

        @Override
        public void addControl(Control control) {
            if (control instanceof Column){
                super.addControl(control);
            }else {
                throw new RuntimeException("Cannot add controls other than Column in a Row Control");
            }
        }
        
    }
    
    public static class Column extends Control { 
        public Column(boolean heading){
            super(heading ? "th" : "td");
        }
    }
    public static class THead extends Control {
		public THead() {
			super("thead");
		}
    }
    public static class TBody extends Control {
		public TBody() {
			super("tbody");
		}
    }
    

}
