package com.venky.swf.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.venky.core.util.ExceptionUtil;

public class MultiException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7536473966344832621L;

	List<Throwable> throwables = new ArrayList<Throwable>();
	public MultiException(Throwable t){
		super("Multiple exeptions found!");
		add(t);
	}
	
	public void add(Throwable t){
		throwables.add(ExceptionUtil.getRootCause(t));
	}
	
	private String newLine(){
		return (System.getProperty("line.separator"));
	}
	@Override
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append(super.toString());
		for (Throwable th: throwables){
			b.append(newLine());
			b.append(th);
		}
		return b.toString();
	}
	
	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);
		s.println();
		if (!throwables.isEmpty()){
			s.println("------------------------");
		}
		for (Throwable th: throwables){
			th.printStackTrace(s);
			s.println();
			s.println("------------------------");
		}
		
	}
	public void printStackTrace(PrintWriter w) {
		super.printStackTrace(w);
		if (!throwables.isEmpty()){
			w.println("------------------------");
		}
		for (Throwable th: throwables){
			th.printStackTrace(w);
			w.println("------------------------");
		}
	}
	
}
