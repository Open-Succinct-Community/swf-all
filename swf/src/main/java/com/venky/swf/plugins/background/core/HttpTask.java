package com.venky.swf.plugins.background.core;

import com.venky.core.util.MultiException;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.task.manager.HttpTaskManager;
import com.venky.swf.routing.jetty.RequestProcessor;
import com.venky.swf.views._IView;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;

public class HttpTask extends RequestProcessor implements Task {
    
    public HttpTask(Request request, Response response, Callback callback) {
        super(request,response,callback);
    }
    public HttpTask(String target, Request request, Response response, Callback callback) {
        super(target,request,response,callback);
    }
    
    public HttpTask(_IPath iPath) {
        super(iPath);
    }
    
    
    @Override
    public void onSuccess() {
        try {
            if (!iView.isBeingForwarded()) {
                Task.super.onSuccess();//Needed for redirected views.
                iView.write();
                iPath.getCallback().succeeded();
            }else {
                iView.write();
                Task.super.onSuccess(); //Just to make sure for forwarded views to run their task after commit.
            }
        } catch (IOException e) {
            log("onSuccess Failed",e);
        }
    }
    
    @Override
    public void onException(Throwable ex) {
        try {
            Task.super.onException(ex);
            iView = null;
            if (iPath.getSession() != null) {
                if (iPath.redirectOnException()) {
                    iPath.addErrorMessage(ex.getMessage());
                    log("Request Failed",ex);
                    if (iPath.getTarget().equals(iPath.getBackTarget())) {
                        iView = router.createRedirectorView(iPath, "/dashboard");
                    } else {
                        iView = router.createRedirectorView(iPath, iPath.getBackTarget());
                    }
                }
            }
            if (iView == null) {
                iView = router.createExceptionView(iPath, ex);
            }
            iView.write();
            iPath.getCallback().succeeded();
        }catch (IOException ioException){
            MultiException multiException = new MultiException();
            multiException.add(ex);
            multiException.add(ioException);
            iPath.getCallback().failed(ioException);
            log("Exception found  ",multiException);
        }
    }
    
    @Override
    public void onComplete() {
        Task.super.onComplete();
    }
    
    @Override
    public void execute() {
        iView = createView();
    }
    
    protected _IView createView(){

        return iPath.invoke(); //Most Expensive!!
    }

    @Override
    public Priority getTaskPriority() {
        return Priority.HIGH;
    }
    
    @Override
    public <W extends AsyncTaskManager> Class<W> getDefaultTaskManagerClass() {
        return (Class<W>)HttpTaskManager.class;
    }
}
