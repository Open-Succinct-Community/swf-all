package com.venky.swf.views.controls;

import java.util.List;

public interface _IControl {
	public void addControl (_IControl child);
	public void setParent (_IControl parent);
	public List<_IControl> getContainedControls();
	public String getTag();
}
