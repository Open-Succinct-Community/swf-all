/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.IS_AUTOINCREMENT;
import com.venky.swf.db.annotations.column.IS_NULLABLE;

/**
 *
 * @author venky
 */
public interface Model {
    @IS_NULLABLE(false)
    @IS_AUTOINCREMENT
    public int getId();
    
    
    public int getLockId();
    public void setLockId(int lockid);
    
    public void save();
    public void destroy();
}
