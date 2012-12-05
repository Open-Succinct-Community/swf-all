package com.venky.swf.plugins.bugs.db.model;

import java.io.Reader;
import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@MENU("Issues")
@HAS_DESCRIPTION_FIELD("TITLE")
public interface Issue extends Model{
	@Index
	public String getTitle();
	public void setTitle(String title);
	
	@Index
	public Reader getDescription();
	public void setDescription(Reader description);
	
	@Enumeration("OPEN,WIP,CLOSED")
	@Index
	public String getStatus();
	public void setStatus(String status);
	
	@Enumeration("P1,P2,P3")
	@Index
	@COLUMN_DEF(value=StandardDefault.SOME_VALUE,args="P2")
	public String getPriority();
	public void setPriority(String priority);
	
	@Enumeration(" ,FIXED,DUPLICATE,WONTFIX,WORKSFORME,BEHAVIOUR_EXPLAINED")
	@Index
	public String getResolution();
	public void setResolution(String resolution);
		
	
	public List<Note> getNotes(); 
}
