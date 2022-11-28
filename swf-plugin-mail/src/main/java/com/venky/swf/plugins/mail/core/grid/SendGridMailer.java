package com.venky.swf.plugins.mail.core.grid;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.mail.core.Mailer;
import com.venky.swf.routing.Config;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.codemonkey.simplejavamail.email.Email;
import org.codemonkey.simplejavamail.email.Recipient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SendGridMailer implements Mailer{
	private static final String WSURL = "http://sendgrid.com/api/mail.send.xml";

	public SendGridMailer() {
		
	}

	public void sendMail(Email mail) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(WSURL);
		List<NameValuePair> attributes = new ArrayList<NameValuePair>();
		try {
			{
				int i = 0 ;
				for (Recipient r : mail.getRecipients()){
					attributes.add(new BasicNameValuePair("to["+ i +"]", r.getAddress()));
					attributes.add(new BasicNameValuePair("toname["+ i +"]", r.getName()));
					i++;
				}
			}
			attributes.add(new BasicNameValuePair("subject", mail.getSubject()));
			if (!ObjectUtil.isVoid(mail.getText())){
				attributes.add(new BasicNameValuePair("text", mail.getText()));
			}else if (!ObjectUtil.isVoid(mail.getTextHTML())){
				attributes.add(new BasicNameValuePair("html", mail.getTextHTML()));
			}
			attributes.add(new BasicNameValuePair("from", Config.instance().getProperty("swf.sendmail.user")));
			attributes.add(new BasicNameValuePair("fromname", Config.instance().getProperty("swf.sendmail.user.name")));
			
			String sendGridAccountUserName = Config.instance().getProperty("swf.sendmail.account");
			String sendGridAccountPassword = Config.instance().getProperty("swf.sendmail.password");
			
			attributes.add(new BasicNameValuePair("api_user",sendGridAccountUserName));
			attributes.add(new BasicNameValuePair("api_key",sendGridAccountPassword));
			
			post.setEntity(new UrlEncodedFormEntity(attributes));
			HttpResponse response = client.execute(post);
			String sResponse = StringUtil.read(response.getEntity().getContent());

			Config.instance().getLogger(getClass().getName()).info(sResponse);
		}catch (IOException e) {
			Config.instance().printStackTrace(getClass(), e);
		}
	}
}
