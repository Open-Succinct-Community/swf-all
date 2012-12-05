package com.venky.swf.plugins.bugs.controller;

import java.util.List;

import com.venky.swf.controller.ModelController;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.bugs.db.model.Note;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.model.ModelListView;

public class NotesController extends ModelController<Note> {

	public NotesController(Path path) {
		super(path);
	}
	
    protected HtmlView createListView(List<Note> records){
    	return new ModelListView<Note>(getPath(), Note.class, new String[]{"NOTES","UPDATED_AT"}, records);
    }

}
