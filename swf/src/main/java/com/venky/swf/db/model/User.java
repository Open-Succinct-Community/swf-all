/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import java.util.List;
import java.util.Map;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;

/**
 *
 * @author venky
 */
@HAS_DESCRIPTION_COLUMN
@CONFIGURATION
@MENU("Admin")
public interface User extends Model{
    public String getName();
    public void setName(String name);
    
    @PASSWORD
    public String getPassword();
    public void setPassword(String password);
    
    public boolean authenticate(String password);
    public static final String USER_AUTHENTICATE = "user.authenticate" ;
   
    public <M extends Model> Map<String,List<Integer>> getParticipationOptions(Class<M> modelClass);
    public Map<String,List<Integer>> getParticipationOptions(Class<? extends Model> modelClass,Model model);
    public static final String GET_PARTICIPATION_OPTION = "get.participation.option";//++ModelClass.SimpleName
    
    public <M extends Model> Expression getDataSecurityWhereClause(Class<M> modelClass);
    public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass,Model model);
    public Expression getDataSecurityWhereClause(ModelReflector<? extends Model> ref, Map<String,List<Integer>> participatingOptions);
    
    @IS_VIRTUAL
    public boolean isAdmin();
    
	@COLUMN_NAME("ID")
	@PROTECTION
	@PARTICIPANT
	@HIDDEN
	@HOUSEKEEPING
	public Integer getSelfUserId();
	public void setSelfUserId(Integer userId);
	@IS_VIRTUAL
	public User getSelfUser();

	@CONNECTED_VIA("USER_ID")
	public List<UserEmail> getUserEmails();
}
