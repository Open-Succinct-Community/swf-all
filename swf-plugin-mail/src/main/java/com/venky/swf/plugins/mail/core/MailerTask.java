package com.venky.swf.plugins.mail.core;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.attachment.db.model.Attachment;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.mail.db.model.Mail;
import com.venky.swf.plugins.mail.db.model.MailAttachment;
import com.venky.swf.plugins.mail.db.model.User;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import org.codemonkey.simplejavamail.email.Email;


import javax.mail.Message.RecipientType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MailerTask implements Task{

	private static final long serialVersionUID = 8083486775891668308L;
	
	long toUserId ;
	String toEmail;
	String subject; 
	String text; 
	boolean isHtml = false;
	List<Long> cc;
	List<Long> bcc;
	List<AttachedElement> attachedElements;

	public static class AttachedElement implements Serializable {
		public AttachedElement(){

		}
		public AttachedElement(MimeType mimeType, String name, byte[] bytes){
			this.bytes = bytes;
			this.name = name;
			this.mimeType = mimeType;
		}
		byte[] bytes;
		String name;
		MimeType mimeType;
	}

	@Deprecated
	public MailerTask(){

	}
	public MailerTask(User to,String subject, String text){
		this(to,null, subject,text);
	}
	public MailerTask(User to,String toEmail,String subject, String text){
		this(to,toEmail,subject,text,null,null,null);
	}
	public MailerTask(User to, String toEmail, String subject, String text, List<User> cc , List<User> bcc, List<AttachedElement> attachedElements){
		this.toUserId = to.getId();
		this.toEmail = toEmail;
		this.subject = subject;
		this.text = text;
		if (!ObjectUtil.isVoid(text)){
			String trim = text.trim();
			int len = trim.length();
			if (len > 5 ) { 
				this.isHtml = trim.substring(0, 5).equalsIgnoreCase("<html") || (len > 14 && trim.substring(0,14).equalsIgnoreCase("<!DOCTYPE html"));
			}
		}
		if (cc != null){
			this.cc = DataSecurityFilter.getIds(cc);
		}
		if (bcc != null){
			this.bcc = DataSecurityFilter.getIds(bcc);
		}
		if (attachedElements != null){
			this.attachedElements = new ArrayList<>(attachedElements);
		}
	}
	
	public void execute() {
		User to = Database.getTable(User.class).get(toUserId);

		if (to == null){
			return;
		}

		List<User> cc = this.cc == null ? new ArrayList<>() : new Select().from(User.class).where(new Expression(ModelReflector.instance(User.class).getPool(),"ID", Operator.IN,this.cc.toArray())).execute();
		List<User> bcc = this.bcc == null ? new ArrayList<>() : new Select().from(User.class).where(new Expression(ModelReflector.instance(User.class).getPool(),"ID", Operator.IN,this. bcc.toArray())).execute();

		Map<RecipientType,List<User>> map = new HashMap<>();

		map.put(RecipientType.TO, Collections.singletonList(to));
		map.put(RecipientType.CC,cc);
		map.put(RecipientType.BCC,bcc);

		List<UserEmail> emails = to.getUserEmails();
		if (!ObjectUtil.isVoid(this.toEmail)){
			emails = emails.stream().filter(e->ObjectUtil.equals(e.getEmail(),this.toEmail)).collect(Collectors.toList());
		}
		if (emails.isEmpty()){
			throw new RuntimeException("No toEmail available for " + to.getName());
		}
		
		String emailId = Config.instance().getProperty("swf.sendmail.user");
		String userName = Config.instance().getProperty("swf.sendmail.user.name");
		
		if( ObjectUtil.isVoid(emailId)) {
			throw new RuntimeException("Plugin not configured :swf.sendmail.user" );
		}
		
		final Email email = new Email();
		email.setFromAddress(userName, emailId);
		email.setSubject(subject);

		StringBuilder emailString = new StringBuilder();
		for (RecipientType type : map.keySet()){
			List<User> users = map.get(type);
			if (!users.isEmpty()){
				emailString.append(type.toString()).append(": ");
				for (User user : users){
					for (UserEmail useremail : user.getUserEmails()){
						email.addRecipient(useremail.getAlias() + "(" + useremail.getEmail() + ")" , useremail.getEmail(), type);
						emailString.append(useremail.getEmail()).append(";");
					}
				}
			}
		}


		if (isHtml){
			email.setTextHTML(text);
		}else {
			email.setText(text);
		}

		Mail mail = Database.getTable(Mail.class).newRecord();
		mail.setUserId(toUserId);
		mail.setEmail(emailString.toString());
		mail.setSubject(subject);
		mail.setBody(new StringReader(text));
		mail.save();

		if (attachedElements != null && !attachedElements.isEmpty()){
			for (AttachedElement element : attachedElements){
				email.addAttachment(element.name, element.bytes, element.mimeType.toString());

				MailAttachment mailAttachment = Database.getTable(MailAttachment.class).newRecord();
				mailAttachment.setMailId(mail.getId());
				Attachment attachment = Attachment.find(element.name);
				if (attachment == null){
					attachment = Database.getTable(Attachment.class).newRecord() ;
					attachment.setAttachment(new ByteArrayInputStream(element.bytes));
					attachment.setAttachmentContentName(element.name);
					attachment.setAttachmentContentSize(element.bytes.length);
					attachment.setAttachmentContentType(element.mimeType.toString());
					attachment.save();
				}
				mailAttachment.setAttachmentId(attachment.getId());
				mailAttachment = Database.getTable(MailAttachment.class).getRefreshed(mailAttachment);
				mailAttachment.save();
			}
		}



		AsyncMailer.instance().addEmail(email);
	}

}
