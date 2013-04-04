package com.venky.swf.urlencode;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.junit.Test;

public class UrlEncodeTest {

	@Test
	public void test() throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {
		//String url = "a b x.xls";
		//String x = URLEncoder.encode(url, "UTF-8");
		
		//System.out.println(new java.net.URI("file","a b c.xls",null).toURL().getPath());
		String x = URLEncoder.encode("\"x\"","UTF-8");
		System.out.println(x);
	}

}
