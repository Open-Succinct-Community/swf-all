/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.login;

import com.venky.swf.routing.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.buttons.Submit;
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
    protected void createBody(Body b) {
        Table table = new Table();

        Label lbluser = new Label("User: ");
        TextBox txtUser = new TextBox();
        txtUser.setProperty("name", "name");

        Row row = table.createRow();
        row.createColumn().addControl(lbluser);
        row.createColumn().addControl(txtUser);
        
        Label lblPassword = new Label("Password: ");
        PasswordText txtPassword = new PasswordText();
        txtPassword.setProperty("name", "password");
        
        row = table.createRow();
        row.createColumn().addControl(lblPassword);
        row.createColumn().addControl(txtPassword);
        
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
    }
    
}
