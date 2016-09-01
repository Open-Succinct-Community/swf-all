package com.venky.swf.plugins.background.controller;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.db.model.Trigger;
import com.venky.swf.routing.Config;
import com.venky.swf.views.View;

public class TriggersController extends VirtualModelController<Trigger>{

	public TriggersController(Path path) {
		super(path);
	}
	public View performAction(String action){
		Trigger model = null;
		Level level = Level.INFO;
		try {
			if (!getPath().getRequest().getMethod().equalsIgnoreCase("POST")) {
				throw new RuntimeException("Can call only as a POST");
			}
			List<Trigger> runs = getIntegrationAdaptor().readRequest(getPath());
			model = runs.isEmpty() ? null : runs.get(0);
			if (model == null || ObjectUtil.isVoid(model.getAgentName()) ) {
				throw new RuntimeException("Don't know which agent to trigger");
			}
			
			if (model != null) {
				if ("fire".equals(action)) {
					Agent.instance().start(model.getAgentName());
				}else if ("halt".equals(action)){
					Agent.instance().finish(model.getAgentName());
				}
			}
			return getIntegrationAdaptor().createStatusResponse(getPath(), null);
		} catch (Exception ex) {
			level = Level.WARNING;
			return getIntegrationAdaptor().createStatusResponse(getPath(), ex);
		} finally {
			write(model, level);
		}
		
	}
	
	public View fire() {
		return performAction("fire");
	}
	
	public View halt() {
		return performAction("halt");
	}
	
	public void write(Trigger model, Level level) {
		if (model == null) {
			return ;
		}
		FormatHelper<Object> helper = FormatHelper.instance(getIntegrationAdaptor().getMimeType(),
				getModelClass().getSimpleName(), false);
		Object element = helper.getRoot();
		Object attribute = helper.getElementAttribute("Trigger");
		if (attribute == null) {
			attribute = element;
		}
		ModelWriter<Trigger, Object> writer = ModelIOFactory.getWriter(getModelClass(), helper.getFormatClass());
		writer.write(model, attribute, Arrays.asList("AGENT_NAME"));
		cat.log(level, element.toString());
	}

	private static final Logger cat = Config.instance().getLogger(TriggersController.class.getName());
	
}
