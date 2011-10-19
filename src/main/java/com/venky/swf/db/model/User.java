/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;

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
}
