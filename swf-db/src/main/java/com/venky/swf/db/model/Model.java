/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_AUTOINCREMENT;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.exceptions.AccessDeniedException;

/**
 *
 * @author venky
 */
public interface Model extends _Identifiable {
    @IS_NULLABLE(false)
    @IS_AUTOINCREMENT
    @HIDDEN
    @HOUSEKEEPING
    public long getId();
    public void setId(long id);
    
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
	public Long getUpdaterUserId();
	public void setUpdaterUserId(Long updaterUserId);
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
	public Long getCreatorUserId();
	public void setCreatorUserId(Long creatorUserId);
	public User getCreatorUser();
	
	@COLUMN_NAME("created_at")
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.CURRENT_TIMESTAMP)
	@PROTECTION
	@HOUSEKEEPING
	public Timestamp getCreatedAt();
	public void setCreatedAt(Timestamp createdAt);


	public void preValidate();
	/**
	 * Calls {@link #save(boolean)} with true.
	 */
    public void save();
    /** 
     * Provided for performance during installations. 
     * This method is not recommended to be be used in applications generally.
     * @see #save() 
     * @param performValidations may be passed as false to bypass validations.
     */
    public void save(boolean performValidations);
	public void save(boolean performValidations,boolean dryRun);
    public void destroy();
    public void init();
    public boolean isAccessibleBy(User user);
    public boolean isAccessibleBy(User user, Class<? extends Model> asModel);
    
    public Set<String> getParticipatingRoles(User user) throws AccessDeniedException;
    public Set<String> getParticipatingRoles(User user,Class<? extends Model> asModel) throws AccessDeniedException;
    public Set<String> getParticipatingRoles(User user,Cache<String,Map<String,List<Integer>>> pGroupOptions) throws AccessDeniedException;
    
    public Record getRawRecord();
    public void setRawRecord(Record record);
    public <M extends Model> M cloneProxy();

    
    public <T> T getTxnProperty(String name);
    public <T> void setTxnProperty(String name,T value);
    public <T> T removeTxnProperty(String name);
    
    @IS_VIRTUAL
    @HIDDEN
    public boolean isDirty();
    
    public void defaultFields();
    
    
    public <T extends Model> ModelReflector<T> getReflector(); 
}
