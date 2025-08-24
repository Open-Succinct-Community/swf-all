package com.venky.swf.views;

import java.io.IOException;

public interface _IView {
	default boolean isBeingForwarded(){
		return false;
	}
	default boolean isBeingRedirected(){
		return false;
	}
	public void write() throws IOException;
    public void write(int httpStatusCode) throws IOException;
}
