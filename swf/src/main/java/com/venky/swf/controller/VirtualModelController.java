package com.venky.swf.controller;

import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;

public abstract class VirtualModelController<M extends Model> extends ModelController<M> {

	public VirtualModelController(Path path) {
		super(path);
	}

	private View throwOperationNotSupportedException(){
		throw new UnsupportedOperationException("Action not supported for:" + getModelClass().getName());
	}
	@Override
	public View exportxls() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View importxls() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View index() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View search() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View show(int id) {
		return throwOperationNotSupportedException();
	}

	@Override
	public View view(int id) {
		return throwOperationNotSupportedException();
	}

	@Override
	public View edit(int id) {
		return throwOperationNotSupportedException();
	}

	@Override
	public View clone(int id) {
		return throwOperationNotSupportedException();
	}

	@Override
	public View blank() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View truncate() {
		return throwOperationNotSupportedException();
	}

	@Override
	public View destroy(int id) {
		return back();
	}

	@Override
	public View save() {
		return back();
	}

}
