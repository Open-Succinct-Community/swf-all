/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import java.sql.Timestamp;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_AUTOINCREMENT;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;

/**
 *
 * @author venky
 */
public interface Model {
    @IS_NULLABLE(false)
    @IS_AUTOINCREMENT
    @HIDDEN
    public int getId();
    public void setId(int id);
    
    @HIDDEN
    public int getLockId();
    public void setLockId(int lockid);
    
	@COLUMN_SIZE(10)
	@COLUMN_NAME("updater_id")
	public Integer getUpdaterUserId();
	public void setUpdaterUserId(Integer updaterUserId);
	public User getUpdaterUser();
	
	@COLUMN_NAME("updated_at")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)
	@HIDDEN
	public Timestamp getUpdatedAt();
	public void setUpdatedAt(Timestamp updatedAt);

	@COLUMN_SIZE(10)
	@COLUMN_NAME("creator_id")
	public Integer getCreatorUserId();
	public void setCreatorUserId(Integer creatorUserId);
	public User getCreatorUser();
	
	@COLUMN_NAME("created_at")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)
	@HIDDEN
	public Timestamp getCreatedAt();
	public void setCreatedAt(Timestamp createdAt);
    	
    public void save();
    public void destroy();
}
