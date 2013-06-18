package com.venky.swf.plugins.bugs.db.model;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.CLONING_PROTECT;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

@MENU("Help")
@HAS_DESCRIPTION_FIELD("TITLE")
public interface Issue extends Model{
	@HIDDEN(false)
	@PROTECTION(Kind.NON_EDITABLE)
	@EXPORTABLE
	public int getId();
	
	@Index
	@CLONING_PROTECT
	@UNIQUE_KEY
	@IS_NULLABLE(false)
	public String getTitle();
	public void setTitle(String title);

	@Enumeration("P1,P2,P3")
	@Index
	@COLUMN_DEF(value=StandardDefault.SOME_VALUE,args="P2")
	public String getPriority();
	public void setPriority(String priority);

	@Index
	@IS_VIRTUAL
	public Reader getDescription();
	public void setDescription(Reader description);
	
	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_WIP = "WIP";
	public static final String STATUS_CLOSED = "CLOSED";
	@Enumeration(STATUS_OPEN+","+STATUS_WIP+","+STATUS_CLOSED)
	@Index
	@COLUMN_DEF(value=StandardDefault.SOME_VALUE,args=STATUS_OPEN)
	public String getStatus();
	public void setStatus(String status);
	
	
	@Enumeration(" ,FIXED,DUPLICATE,WONTFIX,WORKSFORME,BEHAVIOUR_EXPLAINED")
	@Index
	public String getResolution();
	public void setResolution(String resolution);
	
	@IS_NULLABLE
	@CLONING_PROTECT
	public Integer getAssignedToId();
	public void setAssignedToId(Integer id);
	public User getAssignedTo();
	
	@Index
	@COLUMN_NAME("creator_id")
	@EXPORTABLE
	public Integer getCreatorUserId();

	@EXPORTABLE
	public Timestamp getCreatedAt();

	
	@IS_VIRTUAL
	public InputStream getAttachment();
	public void setAttachment(InputStream content);
	
	@HIDDEN
	@IS_VIRTUAL
	public String getAttachmentContentName();
	public void setAttachmentContentName(String name);
	
	@HIDDEN
	@IS_VIRTUAL
	public String getAttachmentContentType();
	public void setAttachmentContentType(String contentType);
	
	public void yank();
	
	
	public List<Note> getNotes(); 
	
}
