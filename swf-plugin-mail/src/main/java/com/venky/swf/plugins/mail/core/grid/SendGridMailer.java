package com.venky.swf.plugins.mail.core.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Recipient;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.mail.core.Mailer;
import com.venky.swf.routing.Config;

public class SendGridMailer implements Mailer{
	private static final String WSURL = "http://sendgrid.com/api/mail.send.xml";

	public SendGridMailer() {
		
	}

	public void sendMail(Email mail) {
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(WSURL);
		List<NameValuePair> attributes = new ArrayList<NameValuePair>();
		Recipient r = mail.getRecipients().get(0);
		try {
			attributes.add(new BasicNameValuePair("to", r.getAddress()));
			attributes.add(new BasicNameValuePair("toname", r.getName()));
			attributes.add(new BasicNameValuePair("subject", mail.getSubject()));
			if (!ObjectUtil.isVoid(mail.getText())){
				attributes.add(new BasicNameValuePair("text", mail.getText()));
			}else if (!ObjectUtil.isVoid(mail.getTextHTML())){
				attributes.add(new BasicNameValuePair("html", mail.getText()));
			}
			attributes.add(new BasicNameValuePair("from", Config.instance().getProperty("swf.sendmail.user")));
			attributes.add(new BasicNameValuePair("fromname", Config.instance().getProperty("swf.sendmail.user.name")));
			
			String sendGridAccountUserName = Config.instance().getProperty("swf.sendmail.account");
			String sendGridAccountPassword = Config.instance().getProperty("swf.sendmail.password");
			
			attributes.add(new BasicNameValuePair("api_user",sendGridAccountUserName));
			attributes.add(new BasicNameValuePair("api_key",sendGridAccountPassword));
			
			// sendgrid.com/api/mail.send.json?to=venky%40shipx.in&toname=Venky&
			// from=threesixtyperf%40succinct.in&fromname=ThreesixtyPerf&subject=Test&text=Test%20Mail%20Text&
			// html=Text%20Mail%20%3Cb%3EHtml%3C%2Fb%3E&api_user=venkatramanm&api_key=succinct12
			post.setEntity(new UrlEncodedFormEntity(attributes));
			HttpResponse response = client.execute(post);
			String sResponse = StringUtil.read(response.getEntity().getContent());

			Config.instance().getLogger(getClass().getName()).info(sResponse);
		}catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
