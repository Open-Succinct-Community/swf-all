package com.venky.swf.routing.jetty;

import com.venky.core.log.SWFLogger;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.Router;
import com.venky.swf.views._IView;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public abstract class RequestProcessor implements Runnable {
    SWFLogger cat = Config.instance().getLogger(getClass().getName());
    protected Router router = Router.instance();
    protected  _IPath iPath;
    protected  _IView iView ;
    
    
    public RequestProcessor(Request request, Response response, Callback callback){
        this(request.getHttpURI().getPath(),request,response,callback);
    }
    public RequestProcessor(String target, Request request, Response response, Callback callback){
        setCustomCnameProcessing(request);
        iPath = router.createPath(target);
        
        iPath.setSession(request.getSession(false));
        iPath.setRequest(request);
        iPath.setResponse(response);
        
        iPath.setCallback(callback);
    }
    public RequestProcessor(_IPath iPath){
        this.iPath = iPath;
    }
    private void setCustomCnameProcessing(Request request){
        String[] hostParams = new String[]{null,null};
        String host = getHeader(request,"Host");
        if (host != null){
            String[] parts = host.split(":");
            for (int i = 0 ; i < Math.min(parts.length,2) ; i ++){
                hostParams[i] = parts[i];
            }
        }
        //Set request based host,port and scheme for this thread.
        Config.instance().setHostName(hostParams[0]);
        Config.instance().setExternalPort(hostParams[1]);
        String extScheme = getHeader(request,"URIScheme");
        if (extScheme == null){
            extScheme = request.getHttpURI().getScheme();
        }
        Config.instance().setExternalURIScheme(extScheme);
    }
    protected String getHeader(Request request,String key) {
        String value = request.getHeaders().get("X-" + key);
        
        if (ObjectUtil.isVoid(value)) {
            value = request.getHeaders().get(key);
        }
        return value;
    }
    
   
}
