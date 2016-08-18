package com.venky.swf.plugins.background.core;

import java.io.Serializable;



public interface Task extends Serializable{
	public void execute();
	
	public static enum Priority {
		HIGH(-1),
		DEFAULT(0),
		LOW(1);
		private final int value; 

		Priority(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
		
	}
}
