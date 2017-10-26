package com.venky.swf.views;

import java.io.IOException;

public interface _IView {
	public boolean isBeingRedirected();
	public void write() throws IOException;
    public void write(int httpStatusCode) throws IOException;
}
