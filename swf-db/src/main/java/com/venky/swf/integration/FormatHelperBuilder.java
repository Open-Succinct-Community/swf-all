package com.venky.swf.integration;

import java.io.InputStream;

public interface FormatHelperBuilder<F> {
	public FormatHelper<F> constructFormatHelper(InputStream in);
	public FormatHelper<F> constructFormatHelper(String root,boolean isPlural);
	public FormatHelper<F> constructFormatHelper(F rootElement);
	
}
