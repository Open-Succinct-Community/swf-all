/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.validations.ExactLength;
import com.venky.swf.db.annotations.column.validations.RegEx;

/**
 *
 * @author venky
 */
public interface User extends Model{
    public String getUsername();
    public void setUsername(String name);
    
    @PASSWORD
    public String getPassword();
    public void setPassword(String password);
    
    @RegEx("[A-z|0-9]+@[A-z|0-9]+[.][A-z]+")
    public String getEmailId();
    public void setEmailId(String emailId);
    
    @RegEx("\\+91[0-9]*")
    @ExactLength(13)
    @COLUMN_SIZE(13)
    public String getMobileNo();
    public void setMobileNo(String mobileNumber);
    
}
