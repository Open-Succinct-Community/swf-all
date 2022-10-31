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
import com.venky.swf.views.controls.page.Image;
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

/**
 *
 * @author venky
 */
public class LoginView extends HtmlView{
	private boolean allowRegistration;
	private boolean newRegistration;

    public LoginView(Path path, boolean allowRegistration, boolean newRegistration){
        super(path);
        this.newRegistration = newRegistration;
		this.allowRegistration = allowRegistration || newRegistration;
	}

	public void addProgressiveWebAppLinks(Column column) {
		String application_name = getApplicationName();
		Image image = getLogo();
		if (image != null) {
			column.addControl(image);
		} else {
			Label appLabel = new Label(application_name);
			column.addControl(appLabel);
			appLabel.addClass("application-title");
		}
	}
	public void addExternalLoginLinks(Column column,String _redirect_to){
		if (!ObjectUtil.isVoid(Config.instance().getClientId("GOOGLE"))){
			column.addControl(new LinkedImage("/resources/images/google-icon.svg","/oid/login?SELECTED_OPEN_ID=GOOGLE" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}
		if (!ObjectUtil.isVoid(Config.instance().getClientId("FACEBOOK"))){
			column.addControl(new LinkedImage("/resources/images/fb-icon.svg","/oid/login?SELECTED_OPEN_ID=FACEBOOK" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}
		if (!ObjectUtil.isVoid(Config.instance().getClientId("LINKEDIN"))){
			column.addControl(new LinkedImage("/resources/images/linkedin-icon.png","/oid/login?SELECTED_OPEN_ID=LINKEDIN" + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
		}
	}
    @Override
    protected void createBody(_IControl b) {

    	String _redirect_to = StringUtil.valueOf(getPath().getFormFields().get("_redirect_to"));

    	FluidContainer loginPanel = new FluidContainer();
    	loginPanel.addClass("application-pannel");
    	b.addControl(loginPanel);
    	
    	Column applicationDescPannel = loginPanel.createRow().createColumn(3,6);
    	applicationDescPannel.addClass("text-center sm:offset-5 sm:col-2 offset-sm-5 col-sm-2");//Bs and tailwind

		addProgressiveWebAppLinks(applicationDescPannel);

		Column extLinks = loginPanel.createRow().createColumn(3,6);

		addExternalLoginLinks(extLinks,_redirect_to);

		Column formHolder = loginPanel.createRow().createColumn(2,8);
		formHolder.addClass("offset-sm-4 col-sm-4 sm:offset-4 sm:col-4");


		Form form = new Form();
        form.setAction(getPath().controllerPath(),"login");
        form.setMethod(Form.SubmitMethod.POST);
        
        formHolder.addControl(form);
        formHolder.addControl(getStatus());

        FormGroup fg = new FormGroup();
		fg.createTextBox(Config.instance().getProperty("Login.Name.Literal","User"), "name",false);
    	form.addControl(fg);

        fg = new FormGroup();
    	fg.createTextBox("Password", "password",true);
    	form.addControl(fg);

    	if (newRegistration){
			fg = new FormGroup();
			fg.createTextBox("Reenter Password", "password2",true);
			form.addControl(fg);
		}

		getPath().getFormFields().forEach((k,v)->{
			TextBox textBox = new TextBox();
			textBox.setVisible(false);
			textBox.setName(k);
			textBox.setValue(v);
			form.addControl(textBox);
		});


        fg = new FormGroup();
		Submit btn = null;
        if (allowRegistration){
        	Submit register = null;
        	if (newRegistration){
				register = fg.createSubmit("Register", 0,12);
				btn = fg.createSubmit("I'm an Existing User",0,12);
				btn.removeClass("btn-primary");
				btn.addClass("btn-link");
			}else {
				btn = fg.createSubmit("Login",0,12);
				register = fg.createSubmit("I'm a new user", 0,12);
				register.removeClass("btn-primary");
				register.addClass("btn-link");
			}
			register.setName("_REGISTER");
		}else {
			btn = fg.createSubmit("Login",3,12,6);
		}
		btn.setName("_LOGIN");

		form.addControl(fg);
		Object error = getPath().getFormFields().get("error");
		Object message = getPath().getFormFields().get("message");
        if (!ObjectUtil.isVoid(error)) {
			setStatus(StatusType.ERROR, error.toString());
		}else if (!ObjectUtil.isVoid(message)){
			setStatus(StatusType.INFO, message.toString());
		}
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
			box.setWaterMark(label);
    			
    		/*Label lbl = new Label(label);
    		lbl.setProperty("for", box.getId());
    		lbl.addClass("col-form-label");
    		lbl.addClass("col-12 com-sm-4 sm:col-4");
    		addControl(lbl);
    		*/
    		Div div = new Div();
    		div.addClass("col-12" );
    		div.addControl(box);
    		
    		addControl(div);
    		return box;
    	}
    	


    	public Submit createSubmit(String label, int offset, int... width){
    		Div div = new Div();
    		div.addClass("offset-"+offset);
			String[] resp = new String[]{"","sm","lg"};
			for (int i = 0 ; i < width.length ; i ++){
				div.addClass(String.format(" col%s%d %scol-%d",resp[i].length() == 0? "-" :"-" + resp[i] + "-" ,width[i], resp[i].length() == 0? "" : resp[i] + ":",width[i]));
			}
    		addControl(div);
    		
    		Submit submit = new Submit(label);
    		submit.addClass("w-100");
    		div.addControl(submit);
    		return submit;
    	}
    	

    }

}
