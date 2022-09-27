package com.venky.swf.plugins.background.eventloop.jetty;

import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.Bucket;
import com.venky.core.util.MultiException;
import com.venky.swf.db._IDatabase;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.IOTask;
import com.venky.swf.plugins.background.eventloop.CoreEvent;
import com.venky.swf.plugins.background.eventloop.IOFuture;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.Router;
import com.venky.swf.views._IView;
import jakarta.servlet.http.HttpSession;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.logging.Level;

public class HttpCoreEvent extends CoreEvent implements _HttpCoreEvent, IOTask {
    Bucket calledNumberOfTimes = new Bucket();
    SWFLogger cat = Config.instance().getLogger(getClass().getName());
    Router router = Router.instance();


    final  String target ;
    final jakarta.servlet.AsyncContext _context;
    _IPath iPath;
    final IOFuture requestHandler;

    public HttpCoreEvent(String target, jakarta.servlet.AsyncContext _context) {
        this.target = target;
        this._context = _context;
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(router.getLoader(), new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if (method.getReturnType().isAssignableFrom(javax.servlet.ServletInputStream.class)){
                        Method m1 = _context.getRequest().getClass().getMethod("getInputStream");
                        return new ServletInputStream(((jakarta.servlet.ServletInputStream) m1.invoke(_context.getRequest(), args)));
                    }else {
                        return method.invoke(_context.getRequest(), args);
                    }
                });

        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(router.getLoader(),new Class[]{HttpServletResponse.class},
                (proxy, method, args) -> method.invoke(_context.getResponse(),args));

        AsyncContext context = (javax.servlet.AsyncContext)  Proxy.newProxyInstance(router.getLoader(),  new Class[]{javax.servlet.AsyncContext.class},
                (proxy, method, args) -> method.invoke(_context,args));

        iPath = router.createPath(target);

        HttpSession jakSession = request.getSession(false);
        javax.servlet.http.HttpSession session = jakSession == null ? null : (javax.servlet.http.HttpSession) Proxy.newProxyInstance(router.getLoader(),  new Class[]{javax.servlet.http.HttpSession.class},
                (proxy, method, args) -> method.invoke(jakSession,args));
        iPath.setSession(session);
        iPath.setRequest(request);
        iPath.setResponse(response);
        iPath.setAsyncContext(context);
        requestHandler =  new IOFuture() {
            Boolean beingForwarded = null;

            @Override
            public Priority getTaskPriority() {
                return Priority.HIGH;
            }

            @Override
            public void execute() {
                if (beingForwarded == null) {
                    beingForwarded = handle();
                }
            }


            public boolean handle() {
                Timer timer = cat.startTimer("handleRequest : " + target ,true);
                try {
                    _IDatabase iDatabase = router.getDatabase();
                    iDatabase.setContext(_IPath.class.getName(),iPath);
                    _IView view = null;
                    try {
                        view = iPath.invoke(); //Most Expensive!!

                        if (iDatabase != router.getDatabase()){
                            iDatabase = router.getDatabase();
                        }
                        if (view.isBeingRedirected()){
                            iDatabase.getCurrentTransaction().commit();
                        }

                        Timer viewWriteTimer = cat.startTimer(target+".view_write", Config.instance().isTimerAdditive());
                        try {
                            view.write();
                        }catch (Exception ex){
                            cat.log(Level.INFO,"Failed to write view. This is not an error mostly it could be due to connection reset!! " + iPath.getTarget() + ":" , ex);
                        }finally{
                            viewWriteTimer.stop();
                        }
                        iDatabase.getCurrentTransaction().commit();
                        return view.isBeingForwarded();
                    }catch(Exception e){
                        try {
                            cat.log(Level.INFO, "Request failed for " + iPath.getTarget() + ":" , e);
                            if (iDatabase != null) {
                                iDatabase.getCurrentTransaction().rollback(e);
                            }
                        }catch (Exception ex){
                            cat.log(Level.INFO, "Rollback failed", ex);
                        }

                        _IView ev ;
                        if (iPath.getSession() != null){
                            if (iPath.redirectOnException()){
                                iPath.addErrorMessage(e.getMessage());
                                Config.instance().getLogger(Router.class.getName()).log(Level.INFO, "Request failed", e);
                                if (iPath.getTarget().equals(iPath.getBackTarget())){
                                    ev = router.createRedirectorView(iPath, "/dashboard");
                                }else {
                                    ev = router.createRedirectorView(iPath, iPath.getBackTarget());
                                  }
                            }else {
                                ev = router.createExceptionView(iPath, e);
                            }
                        }else {
                            ev = router.createExceptionView(iPath, e);
                        }
                        ev.write();
                    }finally {
                        if (iDatabase != null ){
                            iDatabase.close();
                        }
                        if (view != null && !view.isBeingForwarded()) {
                            iPath.autoInvalidateSession();
                        }
                    }
                }catch(Exception ex){
                    throw new RuntimeException(ex);
                }finally{
                    timer.stop();
                    TimerStatistics.dumpStatistics();
                }
                return false;
            }

            @Override
            public Boolean get() {
                return beingForwarded;
            }
        };
    }

    @Override
    public void onStart() {
        try {
            super.onStart();
        }catch (RuntimeException ex){
            //If on start fails,, execute will not be called. Task will get directly completed after onException call.
            try {
                router.createExceptionView(iPath, ex).write();
            }catch (IOException ioException){
                MultiException multiException = new MultiException();
                multiException.add(ex);
                multiException.add(ioException);
                Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Exception found  ",multiException);
            }finally {
                finalizeRequestHandler();
                throw ex;
            }
        }
    }

    @Override
    public void execute() {
        super.execute();
        if (calledNumberOfTimes.intValue() <= 0){
            spawn(requestHandler);
            calledNumberOfTimes.increment();
        }else {
            finalizeRequestHandler();
        }
    }

    private void finalizeRequestHandler() {
        boolean beingForwarded = requestHandler.get();
        if (isReady() && !beingForwarded) {
            try {
                _context.getResponse().flushBuffer();
                _context.complete();
            } catch (Exception ex) {
                //
            }
        }
    }

    public void run(){
        getAsyncTaskManager().addAll(Collections.singleton(this));
    }

    @Override
    public Priority getTaskPriority() {
        return Priority.HIGH;
    }
}
