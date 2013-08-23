/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.login;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.LinkedImage;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class LoginView extends HtmlView{
    public LoginView(Path path){
        super(path);
    }
    @Override
    protected void createBody(_IControl b) {
        String _redirect_to = getPath().getRequest().getParameter("_redirect_to");

    	b.addControl(new LinkedImage("/resources/images/oid.png","/oid/login" + (_redirect_to == null ? "" : "?_redirect_to=" + _redirect_to)));
    	
    	String applicationName = Config.instance().getProperty("swf.application.name", "Application Login");
    	Label appLabel = new Label(applicationName);
    	
        Table table = new Table();
        table.addClass("login");

        Label lbluser = new Label("User: ");
        TextBox txtUser = new TextBox();
        txtUser.setProperty("name", "name");

        Row row = table.createRow();
        row.createColumn(2).addControl(appLabel);
    	
        row = table.createRow();
        row.createColumn().addControl(lbluser);
        row.createColumn().addControl(txtUser);
        
        Label lblPassword = new Label("Password: ");
        PasswordText txtPassword = new PasswordText();
        txtPassword.setProperty("name", "password");
        
        row = table.createRow();
        row.createColumn().addControl(lblPassword);
        row.createColumn().addControl(txtPassword);
        
        if (!ObjectUtil.isVoid(_redirect_to)){
            TextBox hidden = new TextBox();
            hidden.setVisible(false);
            hidden.setName("_redirect_to");
            hidden.setValue(_redirect_to);
            table.createRow().createColumn().addControl(hidden);
        }
        
        Submit sbm = new Submit();
        row = table.createRow();
        Table.Column column = row.createColumn(); 
        column.addControl(sbm);
        column.setProperty("colspan", "2");
        
        Form form = new Form();
        form.setAction(getPath().controllerPath(),"login");
        form.setMethod(Form.SubmitMethod.POST);
        form.addControl(table);
        b.addControl(form);
        b.addControl(getStatus());
        
        
    }
    
}
