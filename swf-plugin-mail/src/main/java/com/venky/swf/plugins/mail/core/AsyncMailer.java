package com.venky.swf.plugins.mail.core;

import java.util.LinkedList;
import java.util.logging.Level;

import org.codemonkey.simplejavamail.Email;
//import org.codemonkey.simplejavamail.Mailer;
//import org.codemonkey.simplejavamail.TransportStrategy;

import com.venky.swf.routing.Config;

public class AsyncMailer extends Thread{
	
	private static AsyncMailer _mailer = null;
	public static AsyncMailer instance(){
		if (_mailer != null){
			return _mailer;
		}
		synchronized (AsyncMailer.class){
			if (_mailer == null){
				_mailer = new AsyncMailer();
				_mailer.start();
			}
		}
		return _mailer;
		
	}
	
	private Mailer mailer = null; 

	private LinkedList<Email> emails = new LinkedList<Email>();
	private AsyncMailer() {
		mailer = MailerFactory.instance().getMailer(Config.instance().getProperty("swf.sendmail.protocol"));
		setDaemon(false);
	}
	public void addEmail(Email email){
		synchronized (emails) {
			emails.add(email);
			emails.notifyAll();
		}
	}
	public Email next(){ 
		synchronized (emails) {
			waitIfQueueIsEmpty();
			return emails.remove();
		}
	}
	public void waitIfQueueIsEmpty(){
		synchronized (emails) {
			while (emails.isEmpty()){
				try { 
					emails.wait();
				}catch(InterruptedException ex){
					//
				}
			}
		}
	}
	public void run(){
		Email email = null;
		while ((email = next()) != null ){
			try {
				mailer.sendMail(email);
			}catch (Exception ex){
				Config.instance().getLogger(getClass().getName()).log(Level.WARNING, "Could not send mail" , ex);
			}
		}
	}
}
