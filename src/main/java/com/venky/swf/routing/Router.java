/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import com.venky.swf.db.Database;
import com.venky.swf.views.ExceptionView;
import com.venky.swf.views.View;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author venky
 */
public class Router extends AbstractHandler {

    public Router() {
    }

    
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session  = request.getSession(false); 
        
        Path p = new Path(target);
        p.setSession(session);
        p.setRequest(request);
        p.setResponse(response);
        
        baseRequest.setHandled(true);
        
        try {
            View view = p.invoke();
            view.write();
            Database.getInstance().getCurrentTransaction().commit();
        }catch(Exception e){
        	try { 
        		Database.getInstance().getCurrentTransaction().rollback();
        	}catch (SQLException ex){
        		ex.printStackTrace();
        	}
            ExceptionView ev = new ExceptionView(p, e);
            ev.write();
        }
    }
}
