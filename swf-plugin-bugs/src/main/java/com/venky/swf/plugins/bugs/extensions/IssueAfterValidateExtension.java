package com.venky.swf.plugins.bugs.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.AfterModelValidateExtension;
import com.venky.swf.plugins.bugs.db.model.Issue;

public class IssueAfterValidateExtension extends AfterModelValidateExtension<Issue> {
	static {
		registerExtension(new IssueAfterValidateExtension());
	}
	@Override
	public void afterValidate(Issue model) {
		if (ObjectUtil.equals(model.getStatus(),Issue.STATUS_CLOSED)){
			if (ObjectUtil.isVoid(model.getResolution())){
				throw new RuntimeException("Please fill Resolution field when closing an issue.");
			}
		}
	}

}
