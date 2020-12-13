package com.venky.swf.plugins.bugs.db.model;

import java.io.InputStream;
import java.io.Reader;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;

public class IssueImpl extends ModelImpl<Issue>{

	public IssueImpl(Issue proxy) {
		super(proxy);
		// TODO Auto-generated constructor stub
	}
	private Reader description;
	public Reader getDescription() {
		return this.description;
	}
	public void setDescription(Reader description) {
		this.description = description;
	} 
	
	public void yank(){
		Issue issue = getProxy();
		issue.setStatus("WIP");
		issue.setAssignedToId(Database.getInstance().getCurrentUser().getId());
		issue.save();
	}
	public boolean isDirty(){
		return super.isDirty() || (description != null && !ObjectUtil.isVoid(StringUtil.read(description))); 
	}

	private InputStream attachment;
	public InputStream getAttachment(){
		return attachment;
	}
	public void setAttachment(InputStream content){
		this.attachment = content;
	}
	
	private String contentName;
	public String getAttachmentContentName(){
		return this.contentName ;
	}
	public void setAttachmentContentName(String name){
		this.contentName = name;
	}
	
	private String contentType ; 
	public String getAttachmentContentType(){
		return contentType;
	}
	public void setAttachmentContentType(String contentType){
		this.contentType = contentType;
	}

	private int contentSize = 0;
	public int getAttachmentContentSize(){
		return this.contentSize;
	}
	public void setAttachmentContentSize(int size){
		this.contentSize = size;
	}
}
