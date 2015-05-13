package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls._IControl;

public class HotLink extends Link{
	
	public HotLink() {
		super();
	}
	
	public HotLink(Link link){
		super();
		for (Object k :link.keySet()){
			if (k.equals("class") || k.equals("id")){
				continue;
			}
			put(k,link.get(k));
		}
		setText(link.getText());
		for (_IControl c:link.getContainedControls()){
			addControl(c);
		}
	}
    
	public HotLink(String tag, String url) {
		super(tag, url);
	}

	public HotLink(String url) {
		super(url);
	}

	private static final long serialVersionUID = 92735365625744460L;
	
	public boolean equals(Object o){
		if (o == null){
			return false;
		}
		if (!(o instanceof HotLink)){
			return false;
		}
		HotLink other = (HotLink)o;
		return other._toString().equals(_toString());
	}
	
	@Override
	public int hashCode(){
		return _toString().hashCode();
	}
	
	protected String _toString(){
		StringBuilder s = new StringBuilder();
		s.append(getUrl()).append("||").append(getText());
		return s.toString();
	}
}
