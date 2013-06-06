package com.venky.swf.plugins.mail.db.model;

import java.io.Reader;
import java.sql.Timestamp;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@MENU("")
public interface SentMail extends Model{
	
	@IS_NULLABLE(false)
	public Integer getUserId();
	public void setUserId(Integer id);
	public User getUser();
	
	public String getEmail();
	public void setEmail(String email);

	public String getSubject();
	public void setSubject(String title);
	
	public Reader getBody();
	public void setBody(Reader sBody);
	
	@COLUMN_NAME("CREATED_AT")
	public Timestamp getSentOn();
	public void setSentOn(Timestamp date);
}
