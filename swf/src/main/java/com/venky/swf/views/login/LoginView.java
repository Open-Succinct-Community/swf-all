/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.login;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.LinkedImage;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.Span;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.DateBox;
import com.venky.swf.views.controls.page.text.Input;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.TextBox;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author venky
 */
public class LoginView extends HtmlView{
	private boolean requiresRegistration;
	private boolean registrationInProgress;

    public LoginView(Path path,boolean requiresRegistration, boolean registrationInProgress){
        super(path);
        this.registrationInProgress = registrationInProgress;
		this.requiresRegistration = requiresRegistration || registrationInProgress;

	}
    @Override
    protected void createBody(_IControl b) {

    	String _redirect_to = StringEscapeUtils.escapeHtml4(StringUtil.valueOf(getPath().getFormFields().get("_redirect_to")));

    	FluidContainer loginPanel = new FluidContainer();
    	loginPanel.addClass("application-pannel");
    	b.addControl(loginPanel);
    	
    	Column applicationDescPannel = loginPanel.createRow().createColumn(4,4);
    	applicationDescPannel.addClass("text-center");
    	

    	String applicationName = Config.instance().getProperty("swf.application.name", "Application Login");
    	Label appLabel = new Label(applicationName);
    	appLabel.addClass("application-title");
		if (!ObjectUtil.isVoid(Config.instance().getClientId("GOOGLE"))){
			appLabel.addControl(new LinkedImage("/resources/images/google-icon.svg","/oid/login?SELECTED_OPEN_ID=GOOGLE" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}
		if (!ObjectUtil.isVoid(Config.instance().getClientId("FACEBOOK"))){
			appLabel.addControl(new LinkedImage("/resources/images/fb-icon.svg","/oid/login?SELECTED_OPEN_ID=FACEBOOK" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}
		if (!ObjectUtil.isVoid(Config.instance().getClientId("LINKEDIN"))){
			appLabel.addControl(new LinkedImage("/resources/images/linkedin-icon.png","/oid/login?SELECTED_OPEN_ID=LINKEDIN" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}

		applicationDescPannel.addControl(appLabel);

        Form form = new Form();
        form.setAction(getPath().controllerPath(),"login");
        form.setMethod(Form.SubmitMethod.POST);
        
        loginPanel.addControl(form);
        loginPanel.addControl(getStatus());

        FormGroup fg = new FormGroup();
    	fg.createTextBox("User", "name",false);
    	form.addControl(fg);

        fg = new FormGroup();
    	fg.createTextBox("Password", "password",true);
    	form.addControl(fg);

    	if (requiresRegistration){
			fg = new FormGroup();
			fg.createTextBox("Reenter Password", "password2",true);
			form.addControl(fg);
		}

        if (!ObjectUtil.isVoid(_redirect_to)){
            TextBox hidden = new TextBox();
            hidden.setVisible(false);
            hidden.setName("_redirect_to");
            hidden.setValue(_redirect_to);
            form.addControl(hidden);

        }

        fg = new FormGroup();
        if (!requiresRegistration){
			Submit btn = fg.createSubmit("Login",5,2);
			btn.setName("_LOGIN");
		}else {
			Submit link = fg.createSubmit("Register",5,2);
			link.setName("_REGISTER");
		}

		form.addControl(fg);
        
    }

    private class FormGroup extends Div{
    	public FormGroup(){
    		addClass("row");
    	}

    	/**
    	 * 
    	 */
    	private static final long serialVersionUID = 4813631487870819257L;

    	
    	public Input createTextBox(String label, String fieldName , boolean password){
    		Input box = password ? new PasswordText() : new TextBox();
    		
    		box.setName(fieldName);
    		box.addClass("form-control");
    			
    		Label lbl = new Label(label);
    		lbl.setProperty("for", box.getId());
    		lbl.addClass("col-form-label");
    		lbl.addClass("offset-3 col-sm-1");
    		
    		Div div = new Div();
    		div.addClass("col-sm-4");
    		div.addControl(box);
    		
    		addControl(lbl);
    		addControl(div);
    		return box;
    	}
    	
    	public CheckBox createCheckBox(String label, String fieldName) {
    		Div div = new Div();
    		div.addClass("offset-4 col-sm-4");
    		addControl(div);
    		
    		Div divcb = new Div();
    		divcb.addClass(".form-check");
    		div.addControl(divcb);
    		
    		Label lblCheckBox = new Label(label);
    		CheckBox cb = new CheckBox();
    		lblCheckBox.addControl(cb);
    		divcb.addControl(lblCheckBox);
    		
    		cb.setName(fieldName);
    		return cb;
    	}
    	public Link createLink(String label,String url, int offset, int width){
			Div div = new Div();
			div.addClass("offset-"+offset+ " col-sm-"+width);
			addControl(div);

			Link submit = new Link(url);
			div.addControl(submit);
			submit.addClass("btn btn-primary");
			submit.setText(label);
			return submit;
		}

    	public Submit createSubmit(String label, int offset, int width){
    		Div div = new Div();
    		div.addClass("offset-"+offset+ " col-sm-"+width);
    		addControl(div);
    		
    		Submit submit = new Submit(label);
    		div.addControl(submit);
    		return submit;
    	}
    	
    	public DateBox createDateBox(String label,String fieldName){
    		DateBox box = new DateBox();
    		
    		box.setName(fieldName);
    		box.addClass("form-control");
    			
    		Label lbl = new Label(label);
    		lbl.setProperty("for", box.getId());
    		lbl.addClass("col-form-label");
    		lbl.addClass("offset-3 col-sm-1");
    		
    		Span span = new Span();
    		span.addClass("input-group-addon");
    		span.addControl(new Glyphicon("glyphicon-calendar","Open Calendar"));
    		
    		Div div = new Div();
    		div.addClass("col-sm-4 input-group date ");
    		div.addControl(box);
    		div.addControl(span);
    		
    		
    		addControl(lbl);
    		addControl(div);
    		return box;
    	}
    }

}
