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
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.LinkedImage;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.text.Input;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class LoginView extends HtmlView{
	private final boolean allowRegistration;
	private final LoginContext context;
	public enum LoginContext {
		LOGIN,
		REGISTER,
		PASSWORD_RESET
	}
	protected boolean   isRegistrationRequired() {
		return Config.instance().getBooleanProperty("swf.application.requires.registration", false);
	}



	public LoginView(Path path,  LoginContext context){
        super(path);
        this.allowRegistration = isRegistrationRequired() || context == LoginContext.REGISTER;
		this.context = context;
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
		for (String provider :Config.instance().getOpenIdProviders()){
			if (!ObjectUtil.isVoid(Config.instance().getClientId(provider))){
				column.addControl(new LinkedImage(String.format("/resources/images/%s.svg",provider),String.format("/oid/login?SELECTED_OPEN_ID=%s",provider) + (ObjectUtil.isVoid(_redirect_to) ? "" : "&_redirect_to=" + _redirect_to)));
			}
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
		extLinks.addClass("text-center flex");

		addExternalLoginLinks(extLinks,_redirect_to);

		Column formHolder = loginPanel.createRow().createColumn(2,8);
		formHolder.addClass("offset-sm-4 col-sm-4 sm:offset-4 sm:col-4");


		Form form = new Form();
        form.setAction(getPath().controllerPath(),getPath().action());
        form.setMethod(Form.SubmitMethod.POST);
        
        formHolder.addControl(form);
        formHolder.addControl(getStatus());

        FormGroup fg = new FormGroup();

		boolean showPasswordFields = true;
		if (!getPath().getFormFields().containsKey("ApiKey")) {
			fg.createTextBox(Config.instance().getProperty("Login.Name.Literal", "User"), "name", false);
			form.addControl(fg);
		}else {
			Input apiKey = fg.createTextBox("ApiKey", "ApiKey", false);
			apiKey.setValue(getPath().getFormFields().get("ApiKey"));
			apiKey.setVisible(false);
			form.addControl(fg);
		}

		showPasswordFields = getPath().getFormFields().containsKey("ApiKey") ||
				context != LoginContext.PASSWORD_RESET;

		if (showPasswordFields) {
			fg = new FormGroup();
			fg.createTextBox("Password", "password", true);
			form.addControl(fg);

			if (context == LoginContext.REGISTER || context == LoginContext.PASSWORD_RESET) {
				fg = new FormGroup();
				fg.createTextBox("Re-enter Password", "password2", true);
				form.addControl(fg);
			}
		}


		getPath().getFormFields().forEach((k,v)->{
			if ( k.equals("_LOGIN") || k.equals("_REGISTER") || k.equals("error") || k.equals("message") || k.equals("_RESET")){
				return;
			}
			TextBox textBox = new TextBox();
			textBox.setVisible(false);
			textBox.setName(k);
			textBox.setValue(v);
			form.addControl(textBox);
		});


        fg = new FormGroup();
		Control btn = null;
		Control register = null;
		Control resetPassword = null;
		if (allowRegistration){
			if (context == LoginContext.PASSWORD_RESET){
				resetPassword = fg.createSubmit("Reset Password",0,12);
				btn = fg.createLink("Oh! I remember now.","/login",0,12);
				register = fg.createLink("I'm a new user", "/register", 0,12);
			}else if (context == LoginContext.REGISTER){
				register = fg.createSubmit("Register", 0,12);
				btn = fg.createLink("I'm an Existing User","/login",0,12);
				resetPassword = fg.createLink("Forgot Password","/users/reset_password" ,0,12);
			}else {
				btn = fg.createSubmit("Login",0,12);
				resetPassword = fg.createLink("Forgot Password","/users/reset_password" ,0,12);
				register = fg.createLink("I'm a new user", "/register" , 0,12);
			}
			resetPassword.setName("_RESET");
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
    	
		public Link createLink (String label, String url, int offset, int ... width){
			Div div = new Div();
			div.addClass("offset-"+offset);
			String[] resp = new String[]{"","sm","lg"};
			for (int i = 0 ; i < width.length ; i ++){
				div.addClass(String.format(" col%s%d %scol-%d", resp[i].isEmpty() ? "-" :"-" + resp[i] + "-" ,width[i], resp[i].isEmpty() ? "" : resp[i] + ":",width[i]));
			}
			addControl(div);

			Link link = new Link();
			link.setUrl(url);
			link.setText(label);
			link.addClass("w-full text-right block" );
			link.addClass("btn-link");
			div.addControl(link);

			return link;

		}

    	public Submit createSubmit(String label, int offset, int... width){
    		Div div = new Div();
    		div.addClass("offset-"+offset);
			String[] resp = new String[]{"","sm","lg"};
			for (int i = 0 ; i < width.length ; i ++){
				div.addClass(String.format(" col%s%d %scol-%d", resp[i].isEmpty() ? "-" :"-" + resp[i] + "-" ,width[i], resp[i].isEmpty() ? "" : resp[i] + ":",width[i]));
			}
    		addControl(div);
    		
    		Submit submit = new Submit(label);
    		submit.addClass("w-full p-1");
    		div.addControl(submit);
    		return submit;
    	}
    	

    }

}
