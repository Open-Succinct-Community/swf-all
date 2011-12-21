/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import java.util.List;
import java.util.Map;

import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.sql.Expression;

/**
 *
 * @author venky
 */
@HAS_DESCRIPTION_COLUMN
public interface User extends Model{
    public String getName();
    public void setName(String name);
    
    @PASSWORD
    public String getPassword();
    public void setPassword(String password);
    
    public boolean authenticate(String password);
    public static final String USER_AUTHENTICATE = "user.authenticate" ;
   
    
    public <M extends Model> Map<String,List<Integer>> getParticipationOptions(Class<M> modelClass);
    public static final String GET_PARTICIPATION_OPTION = "get.participation.option";//++ModelClass.SimpleName
    
    public <M extends Model> Expression getDataSecurityWhereClause(Class<M> modelClass);
    
}
