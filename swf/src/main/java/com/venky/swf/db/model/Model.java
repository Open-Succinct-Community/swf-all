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
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.table.Record;

/**
 *
 * @author venky
 */
public interface Model extends _Identifiable {
    @IS_NULLABLE(false)
    @IS_AUTOINCREMENT
    @HIDDEN
    @HOUSEKEEPING
    public int getId();
    public void setId(int id);
    
    @HIDDEN
    @HOUSEKEEPING
    @IS_NULLABLE(false)
    @COLUMN_DEF(StandardDefault.ZERO)
    public int getLockId();
    public void setLockId(int lockid);
    
	@COLUMN_SIZE(10)
	@COLUMN_NAME("updater_id")
	@PROTECTION
	@HOUSEKEEPING
	@COLUMN_DEF(StandardDefault.CURRENT_USER)
	public Integer getUpdaterUserId();
	public void setUpdaterUserId(Integer updaterUserId);
	public User getUpdaterUser();
	
	@COLUMN_NAME("updated_at")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)
	@PROTECTION
	@HOUSEKEEPING
	public Timestamp getUpdatedAt();
	public void setUpdatedAt(Timestamp updatedAt);
	public static final String UPDATED_AT_COLUMN_NAME = "UPDATED_AT";

	@COLUMN_SIZE(10)
	@COLUMN_NAME("creator_id")
	@PROTECTION
	@HOUSEKEEPING
	@COLUMN_DEF(StandardDefault.CURRENT_USER)
	public Integer getCreatorUserId();
	public void setCreatorUserId(Integer creatorUserId);
	public User getCreatorUser();
	
	@COLUMN_NAME("created_at")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)
	@PROTECTION
	@HOUSEKEEPING
	public Timestamp getCreatedAt();
	public void setCreatedAt(Timestamp createdAt);
    	
    public void save();
    public void destroy();
    public void init();
    public boolean isAccessibleBy(User user);
    public boolean isAccessibleBy(User user, Class<? extends Model> asModel);
    public Record getRawRecord();
    public <M extends Model> M cloneProxy();
    
    public String getTxnProperty(String name);
    public void setTxnPropery(String name,String value);
    public String removeTxnProperty(String name);
}
