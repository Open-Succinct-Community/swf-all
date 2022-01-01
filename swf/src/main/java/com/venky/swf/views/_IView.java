package com.venky.swf.views;

import java.io.IOException;

public interface _IView {
	public boolean isBeingRedirected();
	default boolean isBeingForwarded(){
		return false;
	}
	public void write() throws IOException;
    public void write(int httpStatusCode) throws IOException;
}
