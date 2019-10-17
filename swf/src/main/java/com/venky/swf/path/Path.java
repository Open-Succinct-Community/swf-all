/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.path;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.core.date.DateUtils;
import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.cache.Cache;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model._Identifiable;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.parser.SQLExpressionParser;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views._IView;
import com.venky.swf.views.controls.model.ModelAwareness;

/**
 *
 * @author venky
 */
public class Path implements _IPath{
    static {
        Config.instance().getLogger(Path.class.getName()).info("Loaded by " + Path.class.getClassLoader());
    }

    private List<String> pathelements = new ArrayList<String>();
    private String controllerClassName = null;
    private int controllerPathIndex = 0;
    private int actionPathIndex = 1 ; 
    private int parameterPathIndex = 2; 
    private String target = null;
    private HttpSession session = null ;
    private HttpServletRequest request = null ;
    private HttpServletResponse response = null ;
    
    private Map<String,Object> formFields = null;
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFormFields(){
        if (formFields != null){
            return formFields;
        }
        formFields = new HashMap<String,Object>();
        Map<String,Object> formInput = new HashMap<String, Object>();
        HttpServletRequest request = getRequest();
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        if (isMultiPart){
            FileItemFactory factory = new DiskFileItemFactory(1024*1024*128, new File(System.getProperty("java.io.tmpdir")));
            ServletFileUpload fu = new ServletFileUpload(factory);
            try {
                List<FileItem> fis = fu.parseRequest(request);
                for (FileItem fi:fis){
                    if (fi.isFormField()){
                        if (!formInput.containsKey(fi.getFieldName())){
                            formInput.put(fi.getFieldName(), fi.getString());
                        }
                    }else {
                        byte[] content = StringUtil.readBytes(fi.getInputStream());
                        if (content == null || content.length == 0){
                            content = null;
                        }else {
                            formInput.put(fi.getFieldName() + "_CONTENT_TYPE", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fi.getName()));
                            formInput.put(fi.getFieldName() + "_CONTENT_NAME", fi.getName());
                            formInput.put(fi.getFieldName() + "_CONTENT_SIZE", fi.getSize());
                        }
                        formInput.put(fi.getFieldName(), content == null ? null : new ByteArrayInputStream(content));
                    }
                }
            } catch (FileUploadException e1) {
                throw new RuntimeException(e1);
            } catch (IOException e1){
                throw new RuntimeException(e1);
            }
        }else {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()){
                String name =parameterNames.nextElement();
                formInput.put(name,request.getParameter(name));
            }
        }
        
        for (String key : formInput.keySet()){
            int dotIndex = key.indexOf('.') ; 
            if ( dotIndex < 0){
                this.formFields.put(key, formInput.get(key));
            }else{
                String fieldName = key.substring(dotIndex+1);
                
                String modelNameWithIndex = key.substring(0,dotIndex);
                
                int indexOfOpenBracket = modelNameWithIndex.lastIndexOf('[');
                int indexOfCloseBracket = modelNameWithIndex.indexOf(']',indexOfOpenBracket);
                
                String modelName = modelNameWithIndex.substring(0, indexOfOpenBracket);
                
                Integer index = Integer.valueOf(modelNameWithIndex.substring(indexOfOpenBracket+1,indexOfCloseBracket));
                
                
                SortedMap<Integer,Map<String,Object>> modelRecords =  (SortedMap<Integer, Map<String, Object>>) formFields.get(modelName);
                if (modelRecords == null){
                    modelRecords = new TreeMap<Integer,Map<String,Object>>();
                    formFields.put(modelName, modelRecords);        
                }
                
                Map<String,Object> modelAttributes = modelRecords.get(index);
                if (modelAttributes == null){
                    modelAttributes = new HashMap<String,Object>();
                    modelRecords.put(index, modelAttributes);
                }
                modelAttributes.put(fieldName, formInput.get(key));
            }
        }
        
        return formFields;
    }
    public User getSessionUser(){
        HttpSession session = getSession();
        if (session == null){
            return null;
        }
        
        _Identifiable user = null; 
        try {
            user =  (_Identifiable)session.getAttribute("user");
            return (User)user;
        }catch (ClassCastException ex){
            user = null;
            session.removeAttribute("user");
            Number id = (Number)session.getAttribute("user.id");
            if (id != null){
                Table<User> USER = Database.getTable(User.class);
                if (USER != null){
                    user = USER.get(id.longValue());
                    getSession().setAttribute("user", user);
                }
            }
            return (User)user;
        }
    }
    public Long getSessionUserId(){
        if (getSession() == null){
            return null;
        }
        Number id = (Number)getSession().getAttribute("user.id");
        return id.longValue();
    }

    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    private ByteArrayInputStream inputStream = null;
    public ByteArrayInputStream getInputStream() throws IOException {
        if (inputStream == null){
            inputStream = new ByteArrayInputStream(StringUtil.readBytes(getRequest().getInputStream()));
        }
        return inputStream;
    }

    private static final String ORIGNAL_REQUEST_KEY = "swf.original.request.uri";
    public void setRequest(HttpServletRequest request) {
        this.request = request;
        Object originalUrl = request.getAttribute(ORIGNAL_REQUEST_KEY);
        if (originalUrl == null){
            request.setAttribute(ORIGNAL_REQUEST_KEY,request.getRequestURI());
        }
    }
    
    public String getOriginalRequestUrl(){
        return StringUtil.valueOf(request.getAttribute(ORIGNAL_REQUEST_KEY));
    }
    
    protected void logHeaders(){
        if (request != null){
            List<String> headers = new ArrayList<String>();
            Enumeration<String> names = request.getHeaderNames();
            while(names.hasMoreElements()){
                headers.add(names.nextElement());
            }
            Config.instance().getLogger(Path.class.getName()).info("Request Headers:" + headers.toString());
        }
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }
    public Path constructNewPath(String target){
        Path p = new Path(target);
        p.setSession(getSession());
        p.setRequest(getRequest());
        p.setResponse(getResponse());
        return p;
    }
    public List<String> parsePathElements(String target){
        boolean isResource = false;
        List<String> pathComponents = new ArrayList<>();
        StringTokenizer pathTokenizer = new StringTokenizer(target, "/");
        StringBuilder resourcePath = new StringBuilder();
        while (pathTokenizer.hasMoreTokens()) {
            String token = pathTokenizer.nextToken();
            if (pathComponents.isEmpty()) {
                if (token.equals("resources")) {
                    isResource = true;
                }
            }
            if ((token.contains(".") && !pathTokenizer.hasMoreElements())) {
                isResource = true;
                if (resourcePath.length() == 0) {
                    pathComponents.forEach(pe -> resourcePath.append("/").append(pe));
                }
            }
            if (isResource && !token.equals("resources")) {
                resourcePath.append("/").append(token);
            }
            pathComponents.add(token);
        }

        if (isResource) {
            if (!ObjectUtil.isVoid(resourcePath.toString())) {
                try {
                    URL resource = getClass().getResource(resourcePath.toString());
                    if (resource == null) {
                        isResource = false;
                    }
                } catch (Exception ex) {
                    isResource = false;
                }
            } else {
                isResource = false;
            }
            if (isResource) {
                pathComponents.clear();
                pathComponents.add("resources");
                pathComponents.add(resourcePath.toString());
            }
        }
        checkPathOverrides(pathComponents);
        return pathComponents;
    }

    private void checkPathOverrides(List<String> pathComponents) {
        Registry.instance().callExtensions("swf.before.routing", pathComponents);
    }


    public Path(String target) {
        this.target = target;
        pathelements = parsePathElements(target);
        boolean isResource = pathelements.isEmpty()? false : pathelements.get(0).equals("resources");

        int pathElementSize = pathelements.size();
        for (int i = 0 ; !isResource && i < pathElementSize ; i++){
            String token = pathelements.get(i);
            try {
                Long.valueOf(token);
                continue; //Ignore integer elements;
            }catch (NumberFormatException e){
                //
            }
            
            String controllerClassName = ControllerCache.instance().get(token);
            if (controllerClassName == null){
                continue;
            }
            ControllerInfo info = new ControllerInfo(token,controllerClassName);
            info.setControllerPathIndex(i);
            controllerElements.add(info);
            
            if (i < pathElementSize -1){
                info.setAction(pathelements.get(i+1));
                i+= 1;
            }
            try {
                if (i < pathElementSize - 1){
                    info.setParameter(Long.valueOf(pathelements.get(i+1)));
                    i+=1;
                }
            }catch (NumberFormatException ex){
                //It is possible that the parameter is a string one. So lets check the next parameter to see if it is a model or this is 
                //the last pathelement.
                if (i  < pathElementSize - 2){
                    String nextElement =  pathelements.get(i+2);
                    try {
                        Integer.valueOf(nextElement);
                    }catch(NumberFormatException nfe){
                        if (ControllerCache.instance().get(nextElement) != null){
                            info.setParameter(pathelements.get(i+1));
                            i ++ ;
                        }
                    }
                }else {
                    //Last pathelement.
                    info.setParameter(pathelements.get(i+1));
                    i++;
                }
            }
        }
        if (pathElementSize == 0){
            pathelements.add("index");
        }
        loadControllerClassName();
    }

    public boolean isAppAuthenticated() {
        boolean appAuthRequired = getProtocol() != MimeType.TEXT_HTML  && Config.instance().getBooleanProperty("swf.application.authentication.required",false);
        if (!appAuthRequired){
            return true;
        }
        String authorization = getRequest().getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            if (values.length == 2){
                String appId = values[0];
                String plainPass = values[1];
                Application app = Application.find(appId);
                if (ObjectUtil.equals(app.getEncryptedSecret(plainPass),app.getSecret())){
                    return true;
                }
            }
        }
        return false;
    }

    public static class ControllerInfo {
        private String action = null; 
        private Object parameter = null; 
        private Class<? extends Model> modelClass = null;
        private Class<?> controllerClass = null;
        private int controllerPathIndex = -1;
        public ControllerInfo(String controllerName, String controllerClassName){
            this.controllerClass = Path.getClass(controllerClassName);
            this.modelClass = Path.getModelClass(controllerName);
        }

        public Class<?> getControllerClass() {
            return controllerClass;
        }
        public Class<? extends Model> getModelClass(){ 
            return modelClass;
        }
        
        public String getAction() {
            return action;
        }
        public void setAction(String action) {
            this.action = action;
        }
        @SuppressWarnings("unchecked")
        public <T> T getParameter() {
            return (T) parameter;
        }
        public <T> void setParameter(T parameter) {
            this.parameter = parameter;
        }

        public Long getId() {
            if (parameter == null){
                return null;
            }
            if (parameter instanceof Long){
                return (Long)parameter;
            }
            return null;
        }

        public int getControllerPathIndex() {
            return controllerPathIndex;
        }

        public void setControllerPathIndex(int controllerPathIndex) {
            this.controllerPathIndex = controllerPathIndex;
        }
        
    }
    private List<ControllerInfo> controllerElements  = new ArrayList<Path.ControllerInfo>();
    public List<ControllerInfo> getControllerElements(){ 
        return controllerElements;
    }
    
    public String getTarget() {
        return target;
    }
    
    
    private void loadControllerClassName(){
        if (controllerClassName != null){
            return;
        }
        if (!controllerElements.isEmpty()){
            ControllerInfo cinfo = controllerElements.get(controllerElements.size() -1 );
            controllerClassName = cinfo.getControllerClass().getName();
            controllerPathIndex = cinfo.getControllerPathIndex();
        }
        
        if (controllerClassName == null) {
            controllerClassName = Controller.class.getName();
            controllerPathIndex = -1;
        }
        actionPathIndex = controllerPathIndex + 1 ;
        parameterPathIndex = controllerPathIndex + 2;
    }
    
    public String controllerPath(){
        if (controllerPathIndex <= pathelements.size() -1){
            StringBuilder p = new StringBuilder();
            for (int i = 0; i<= controllerPathIndex ; i ++){
                p.append("/");
                p.append(pathelements.get(i));
            }
            return p.toString();
        }
        throw new RuntimeException("Controller path could not be determined!");
    }
    public String getBackTarget(){
        return getBackTarget(true);
    }
    private String getBackTarget(boolean checkIfOrigRequest){
        StringBuilder backTarget = new StringBuilder();
        if (checkIfOrigRequest && !getTarget().equals(getOriginalRequestUrl())){
            Path p = constructNewPath(getOriginalRequestUrl());
            return p.getBackTarget();
        }else {
            if (controllerPathIndex > 0 && controllerPathIndex < pathelements.size()) {
                for (int i = 0 ; i < controllerPathIndex ; i ++  ){
                    backTarget.append("/");
                    backTarget.append(pathelements.get(i));
                }
            }
            if (backTarget.length() == 0){
                backTarget.setLength(0);
                backTarget.append(controllerPath()).append("/index");
                return backTarget.toString();
            }
        }
        
        
        String url = backTarget.toString();
        
        if (url.endsWith("/index")){ // http://host:port/..../controller/index
            Path backPath = constructNewPath(url);
            if (backPath.getModelClass() != null){
                Path twobackPath = constructNewPath(backPath.getBackTarget(false));
                if (twobackPath.getModelClass() != null){
                    ModelReflector<? extends Model> bmr = ModelReflector.instance(twobackPath.getModelClass());
                    for (Class<? extends Model> childModel : bmr.getChildModels(true, true)){
                        if (ModelReflector.instance(childModel).reflects(backPath.getModelClass())){
                            url = twobackPath.getTarget() + "?_select_tab="+ StringEscapeUtils.escapeHtml4(new ModelAwareness(backPath, null).getLiteral(backPath.getModelClass().getSimpleName()));
                            break;
                        }
                    }
                }
            }    
        }

        return url;
    }
    public String controllerPathElement(){
        if (controllerPathIndex <= pathelements.size() - 1){
            if (controllerPathIndex >= 0){
                return pathelements.get(controllerPathIndex);
            }else {
                return "";
            }
        }
        throw new RuntimeException("Controller pathelement could not be determined!");
    }

    public <M extends Model> Class<M> getModelClass(){
        return getModelClass(controllerPathElement());
    }
    
    public static <M extends Model> Class<M> getModelClass(String pathElement){
        Table<M> table = getTable(pathElement);
        if (table == null){
            return null;
        }else {
            return table.getModelClass();
        }
    }
    public static <M extends Model> Table<M> getTable(String pathElement){
        String camelPathElement = StringUtil.camelize(pathElement);
        if (StringUtil.pluralize(camelPathElement).equals(camelPathElement)){
            String tableName = Table.tableName(StringUtil.singularize(camelPathElement));
            Table<M> table = Database.getTable(tableName);
            return table;
        }
        return null;
    }
    
    public String action() {
        String action = "index";
        if (actionPathIndex <= pathelements.size() - 1) {
            action = pathelements.get(actionPathIndex);
        }
        return action;
    }

    public String parameter() {
        String parameter = null;
        if (parameterPathIndex <= pathelements.size() - 1) {
            parameter = pathelements.get(parameterPathIndex);
        }
        return parameter;
    }

    private Controller createController() {
        try {
            return (Controller)getControllerClass().getConstructor(Path.class).newInstance(this);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
    
    public static final String ALLOW_CONTROLLER_ACTION = "allow.controller.action" ; 
    
    public boolean isUserLoggedOn(){
        User currentUser =  Database.getInstance().getCurrentUser();
        if (currentUser == null){
            User sessionUser = getSessionUser();
            if (sessionUser != null){
                Database.getInstance().open(sessionUser);
            }
            currentUser = Database.getInstance().getCurrentUser();
        }
        
        return currentUser != null ; 
    }

    public void createUserSession(User user,boolean autoInvalidate){
        invalidateSession();
        HttpSession session = getRequest().getSession(true);
        if (user != null){
            session.setAttribute("user", user);
            session.setAttribute("user.id",user.getId());
        }
        session.setAttribute("autoInvalidate", autoInvalidate);
        setSession(session);
    }

    public static final String REQUEST_AUTHENTICATOR = "request.authenticator";

    public boolean isRequestAuthenticated(){
        if (isUserLoggedOn()){
            return true;
        }

        if (!isAppAuthenticated()){
            return false;
        }

        ObjectHolder<User> userObjectHolder = new ObjectHolder<>(null);
        Registry.instance().callExtensions(Path.REQUEST_AUTHENTICATOR,this, userObjectHolder);

        User user = userObjectHolder.get();
        boolean autoInvalidate = false;
        if (user == null && !getErrorMessages().isEmpty()){
            return false;
        }

        IntegrationAdaptor<User,?> adaptor = null;
        if (getProtocol() != MimeType.TEXT_HTML) {
            adaptor = IntegrationAdaptor.instance(User.class, FormatHelper.getFormatClass(getProtocol()));
        }
        autoInvalidate = adaptor != null || !ObjectUtil.isVoid(getHeader("ApiKey"));
        if (user == null){
            if (getRequest().getMethod().equalsIgnoreCase("POST")){
                String username = null;
                String password = null;
                if (adaptor == null){
                    Map<String,Object> map = getFormFields();
                    username = String.valueOf(map.get("name"));
                    password = String.valueOf(map.get("password"));
                    String password2 = String.valueOf(map.get("password2"));
                    if (map.containsKey("_REGISTER")){
                        if (ObjectUtil.equals(password,password2)){
                            user = getUser("name",username);
                            if (user != null){
                                addErrorMessage("Username "+ username + " is already registered");
                                return  false;
                            }
                            user = Database.getTable(User.class).newRecord();
                            user.setName(username);
                            user.setPassword(password);
                            user.save();
                        }else {
                            addErrorMessage("Passwords entered do not match");
                            return false;
                        }
                    }
                }else {
                    List<User> input = adaptor.readRequest(this);
                    if (input.size() == 1){
                        Database.getInstance().getCache(ModelReflector.instance(User.class)).clear();
                        username = input.get(0).getName();
                        password = input.get(0).getPassword();
                    }
                }
                if (!ObjectUtil.isVoid(username)){
                    Config.instance().getLogger(Path.class.getName()).fine("Logging in " + username);
                    user = getUser("name",username);
                    Config.instance().getLogger(Path.class.getName()).fine("User is valid ? " + (user != null));
                    if (user != null && user.authenticate(password)){
                        createUserSession(user,autoInvalidate);
                    }else {
                        addErrorMessage("Login Failed");
                        createUserSession(null,true);
                        Config.instance().getLogger(Path.class.getName()).fine("Login Failed");
                    }
                }
            }
        }else {
            createUserSession(user,autoInvalidate);
        }
        
        boolean loggedOn= isUserLoggedOn();
        if (!loggedOn){
            addErrorMessage("Login Failed");
        }
        return loggedOn;
    }
    public boolean redirectOnException(){
        return getProtocol().equals(MimeType.TEXT_HTML);
    }
    
    public MimeType getProtocol(){
        String apiprotocol = getRequest().getHeader("ApiProtocol"); // This is bc.
        if (ObjectUtil.isVoid(apiprotocol)) {
        	apiprotocol = getRequest().getHeader("content-type");
        }
        return Path.getProtocol(apiprotocol);
    }
    public static MimeType getProtocol(String apiprotocol){
        if (ObjectUtil.isVoid(apiprotocol)){
            return MimeType.TEXT_HTML;
        }
        MimeType protocol = MimeType.TEXT_HTML;
        for (MimeType mt : MimeType.values()){
            if (mt.toString().equals(apiprotocol)){
                protocol = mt; 
                break;
            }
        }
        return protocol;
    }
    
    //Can be cast to any user model class as the proxy implements all the user classes.
    public User getUser(String fieldName, String fieldValue){
        Select q = new Select().from(User.class);
        String nameColumn = ModelReflector.instance(User.class).getColumnDescriptor(fieldName).getName();
        q.where(new Expression(q.getPool(),nameColumn,Operator.EQ,new BindVariable(q.getPool(),fieldValue)));
        
        List<? extends User> users  = q.execute(User.class);
        if (users.size() == 1){
            return users.get(0);
        }
        return null;
    }
    
    public User getGuestUser(){
        String guestUserName = Config.instance().getProperty("swf.guest.user");
        if (!ObjectUtil.isVoid(guestUserName)){
        	Select userSelect = new Select().from(User.class);
            List<User> guests = userSelect.where(new Expression(userSelect.getPool(),"NAME",Operator.EQ,guestUserName)).execute(User.class);
            if (guests.size() == 1){
                return guests.get(0);
            }                            
        }
        return null;
    }
    public boolean isGuestUserLoggedOn(){
        String guestUserName = Config.instance().getProperty("swf.guest.user");
        User u = getSessionUser(); 
        if (u != null && ObjectUtil.equals(u.getName(),guestUserName)){
            return true;
        }
        return false;
    }

    private final SWFLogger cat = Config.instance().getLogger(getClass().getName());
    public _IView invoke() throws AccessDeniedException{
        MultiException ex = null;
        List<Method> methods = getActionMethods(action(), parameter());
        for (Method m :methods){
            Timer timer = cat.startTimer(null,Config.instance().isTimerAdditive()); 
            try {
                boolean securedAction = getControllerReflector().isSecuredActionMethod(m) ;
                if (securedAction){
                    if (!isRequestAuthenticated()){
                        User guest = getGuestUser();
                        if (guest != null){
                            createUserSession(guest, false);
                        }
                        
                        if(!isRequestAuthenticated()) {
                            if (getProtocol() == MimeType.TEXT_HTML){
                                return new RedirectorView(this,"","login");
                            }else {
                                throw new AccessDeniedException ("Request not authenticated");
                            }
                        }
                    }
                    ensureControllerActionAccess();
                }
                Controller controller = createController();
                try {
                    if (m.getParameterTypes().length == 0 && parameter() == null){
                        return (View)m.invoke(controller);
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == String.class && parameter() != null){
                        return (View)m.invoke(controller, parameter());
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == int.class && parameter() != null){
                        return (View)m.invoke(controller, Integer.valueOf(parameter()));
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == long.class && parameter() != null){
                        return (View)m.invoke(controller, Long.valueOf(parameter()));
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    if (ex == null){
                        ex = new MultiException();
                    }
                    ex.add(e);
                }
            }finally{
                timer.stop();
            }
        }
        if (ex != null){
            throw ex;
        }
        if (!isUserLoggedOn()){
            return new RedirectorView(this,"","login");
        }
        throw new RuntimeException("Donot know how to invoke controller action " + getTarget()) ;
    }
    
    public boolean canAccessControllerAction(){
        return canAccessControllerAction(action());
    }
    public boolean canAccessControllerAction(String actionPathElement){
        return canAccessControllerAction(actionPathElement,parameter());
    }
    private ControllerReflector<? extends Controller> cref = null; 
    public ControllerReflector<? extends Controller> getControllerReflector(){
        if (cref == null){
            cref = ControllerReflector.instance(getControllerClass());
        }
        return cref;
    }
    public boolean isActionSecure(String actionPathElement){
        return getControllerReflector().isActionSecure(actionPathElement);
    }
    private boolean isNumber(String parameter){
        if (ObjectUtil.isVoid(parameter)) {
            return false;
        }
        try {
            Double.parseDouble(parameter);
            return true;
        }catch (NumberFormatException nfex){
            return false;
        }
    }

    private static List<Class<?>> supportedNumberClasses = Arrays.asList(long.class,int.class); // Inorder of preference.
    public static boolean isNumberClass(Class<?> aClass){
        return supportedNumberClasses.contains(aClass);
    }
    
    private List<Method> getActionMethods(final String actionPathElement,final String parameterPathElement){
        List<Method> methods = getControllerReflector().getActionMethods(actionPathElement);
        
        final int targetParameterLength = ObjectUtil.isVoid(parameterPathElement)? 0 : 1;
        final boolean parameterIsNumeric = isNumber(parameterPathElement);

        Collections.sort(methods,new Comparator<Method>(){
            public int compare(Method o1, Method o2) {
                int ret = 0 ;
                int s1 = 0 ; int s2 = 0 ; 
                s1 = Math.abs(o1.getParameterTypes().length - targetParameterLength);
                s2 = Math.abs(o2.getParameterTypes().length - targetParameterLength) ;
                ret = s1 - s2; 
                if (ret == 0 && o1.getParameterTypes().length == 1){
                    Class<?> t1 = o1.getParameterTypes()[0];
                    Class<?> t2 = o2.getParameterTypes()[0];
                    if (parameterIsNumeric) {
                        s1 = isNumberClass(t1) ? 0 : 1 ;
                        s2 = isNumberClass(t2) ? 0 : 1 ;
                        ret = s1 - s2;
                        if (ret == 0 && s1 == 0) {
                            //Both are numbers,
                            s1 = supportedNumberClasses.indexOf(t1);
                            s2 = supportedNumberClasses.indexOf(t2);
                            ret = s1 - s2; //Give Preference to long.
                        }
                    }else {
                        s1 = t1.equals(String.class) ? 0 : 1;
                        s2 = t2.equals(String.class) ? 0 : 1;
                        ret = s1 - s2;
                    }
                }
                return ret;
            }
            
        });
        
        return methods;
    }
    public boolean canAccessControllerAction(String actionPathElement,String parameterPathElement){
        boolean accessible =  canAccessControllerAction(getSessionUser(), controllerPathElement(), actionPathElement, parameterPathElement,this);
        if (!accessible) {
            return accessible;
        }
        
        List<Method> methods = getActionMethods(actionPathElement, parameterPathElement);
        for (Method m: methods){
            accessible = false;
            Depends depends = getControllerReflector().getAnnotation(m,Depends.class);
            if (depends != null ){
                accessible = true;
                StringTokenizer tok = new StringTokenizer(depends.value(),",") ;
                while (tok.hasMoreTokens() && accessible){
                    accessible = accessible && canAccessControllerAction(tok.nextToken(),parameterPathElement);
                }
            }else {
                accessible = true ;
            }
            if (accessible){
                break ;
            }
        }
        return accessible;
    }

    public static boolean canAccessControllerAction(User user,String controllerPathElement,String actionPathElement,String parameterPathElement,Path path){
        try {
            ensureControllerActionAccess(user,controllerPathElement,actionPathElement,parameterPathElement,path);
        }catch (AccessDeniedException ex){
            return false;
        }
        return true;
    }
    
    private void ensureControllerActionAccess() throws AccessDeniedException{
        ensureControllerActionAccess(getSessionUser(),controllerPathElement(),action(),parameter(),this); 
    }
    private static void ensureControllerActionAccess(User user,String controllerPathElement,String actionPathElement , String parameterPathElement,Path path) throws AccessDeniedException{
        Registry.instance().callExtensions(ALLOW_CONTROLLER_ACTION, user, controllerPathElement,actionPathElement,parameterPathElement, path);
    }
    

    @SuppressWarnings("unchecked")
    public <T extends Controller> Class<T> getControllerClass() {
        return (Class<T>) getClass(getControllerClassName());
    }

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
    private String getControllerClassName() {
        return controllerClassName;
    }

    public Path createRelativePath(String toUrl){
        String relPath = null; 
        if (parameter() != null){
            relPath = getTarget();
        }else {
            relPath = controllerPath() ;
        }
        
        if (!toUrl.startsWith("/")){
            relPath = relPath + "/" + toUrl;
        }else {
            relPath = relPath + toUrl;
        }
        
        Path path = constructNewPath(relPath); 
        return path;
    }
    
    private List<Method> getReferredModelGetters(Map<String,List<Method>> referredModelGettersMap, String referredTableName){
        List<Method> rmg = referredModelGettersMap.get(referredTableName);
        if (rmg == null){
            rmg = new ArrayList<Method>();
        }
        return rmg;
    }
    public Expression getWhereClause(){
        return getWhereClause(getModelClass());
    }
    public Expression getWhereClause(Class<? extends Model> modelClass){
        Map<String, List<Method>> referredModelGetterMap = new HashMap<String, List<Method>>();
        ModelReflector<? extends Model> reflector = ModelReflector.instance(modelClass);
        Expression where = new Expression(reflector.getPool(),Conjunction.AND);
        for (Method referredModelGetter : reflector.getReferredModelGetters()){
            @SuppressWarnings("unchecked")
            Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
            
            ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
            String referredTableName = referredModelReflector.getTableName();
            
            List<Method> getters = referredModelGetterMap.get(referredTableName);
            if (getters == null){
                getters = new ArrayList<Method>();
                referredModelGetterMap.put(referredTableName, getters);
            }
            
            getters.add(referredModelGetter);
        }
        
        if (referredModelGetterMap.isEmpty()){
            return where;
        }

        List<ControllerInfo> controllerElements = new ArrayList<ControllerInfo>(getControllerElements());
        Collections.reverse(controllerElements);
        Iterator<ControllerInfo> cInfoIter = controllerElements.iterator() ;
        if (cInfoIter.hasNext()){
            cInfoIter.next();// The last model was self.
        }

        Model partiallyFilledModel = Database.getTable(reflector.getModelClass()).newRecord();
        Set<String> modelElementProcessed = new HashSet<String>();
        while(cInfoIter.hasNext()){ 
            ControllerInfo controllerInfo = cInfoIter.next();
            if (controllerInfo.getModelClass() == null){
                continue;
            }
            ModelReflector<? extends Model> ref = ModelReflector.instance(controllerInfo.getModelClass());
            List<Method> referredModelGetters = getReferredModelGetters(referredModelGetterMap, ref.getTableName());
            
            if (referredModelGetters.isEmpty() || controllerInfo.getId() == null || modelElementProcessed.contains(ref.getTableName())){
                continue;
            }
            modelElementProcessed.add(ref.getTableName());
            
            Expression referredModelWhere = new Expression(ref.getPool(),Conjunction.AND);
            Expression referredModelWhereChoices = new Expression(ref.getPool(),Conjunction.OR);

            ModelReflector<?> referredModelReflector = ref;
            for (Method childGetter : referredModelReflector.getChildGetters()){
                Class<? extends Model> childModelClass = referredModelReflector.getChildModelClass(childGetter);
                if (reflector.getClassHierarchies().contains(childModelClass)){
                    CONNECTED_VIA join = referredModelReflector.getAnnotation(childGetter,CONNECTED_VIA.class);
                    if (join == null){
                        for (Method referredModelGetter: referredModelGetters){ 
                            String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
                            String referredModelIdColumnName = reflector.getColumnDescriptor(referredModelIdFieldName).getName();
                            reflector.set(partiallyFilledModel,referredModelIdFieldName,controllerInfo.getId());
                            referredModelWhereChoices.add(new Expression(referredModelReflector.getPool(),referredModelIdColumnName,Operator.EQ,new BindVariable(referredModelReflector.getPool(),controllerInfo.getId())));
                        }
                    }else {
                        String referredModelIdColumnName = join.value();
                        String referredModelIdFieldName =  reflector.getFieldName(referredModelIdColumnName);
                        reflector.set(partiallyFilledModel,referredModelIdFieldName,controllerInfo.getId());
                        referredModelWhereChoices.add(new Expression(referredModelReflector.getPool(),referredModelIdColumnName,Operator.EQ,new BindVariable(referredModelReflector.getPool(),controllerInfo.getId())));
                        if (!ObjectUtil.isVoid(join.additional_join())){
                            SQLExpressionParser parser = new SQLExpressionParser(modelClass);
                            Expression expression = parser.parse(join.additional_join());
                            referredModelWhere.add(expression);
                        }
                    }
                }
            }
            if (!referredModelWhereChoices.isEmpty()){
                referredModelWhere.add(referredModelWhereChoices);
            }
            if (referredModelWhere.getParameterizedSQL().length() > 0){
                where.add(referredModelWhere);
            }
        }
        User user = getSessionUser();
        
        if (user != null){
            Cache<String,Map<String,List<Long>>> pOptions = user.getParticipationOptions(reflector.getModelClass(),partiallyFilledModel);
            if (pOptions.size() >  0){
                Set<String> fields = new HashSet<String>();
                for (String g: pOptions.keySet()){
                    fields.addAll(pOptions.get(g).keySet());
                }
                fields.removeAll(DataSecurityFilter.getRedundantParticipationFields(fields, reflector));
                boolean canFilterInSQL = !DataSecurityFilter.anyFieldIsVirtual(fields, reflector);
                
                if (canFilterInSQL){
                    Expression dsw = user.getDataSecurityWhereClause(reflector,pOptions);
                    if (dsw.getParameterizedSQL().length()> 0){
                        where.add(dsw); 
                    }
                }
            }
        }
        return where;

    }
    @Override
    public void invalidateSession() {
        if (session != null){
            session.invalidate();
            session = null;
        }
    }
    
    public void autoInvalidateSession(){
        if (session != null){
            try { 
                if (ObjectUtil.equals(session.getAttribute("autoInvalidate"),true)){
                    invalidateSession();
                }
            }catch(IllegalStateException ex){
                session = null;
            }
        }
    }
    
    public static final String getControllerPathElementName(Class<? extends Model> modelClass){
        return LowerCaseStringCache.instance().get(Database.getTable(modelClass).getTableName());
    }

    public <M extends Model> Path getModelAccessPath(Class<M> modelClass){
        return pathCache.get(modelClass);
    }
    
    private Cache<Class<? extends Model>, Path> pathCache = new Cache<Class<? extends Model>, Path>() {
        /**
         * 
         */
        private static final long serialVersionUID = -1430185913473112366L;

        @Override
        protected Path getValue(Class<? extends Model> modelClass) {
            Path p = Path.this; 
            if (!p.controllerPathElement().equals(getControllerPathElementName(modelClass))){
                Path newPath = constructNewPath("/" + getControllerPathElementName(modelClass) + "/index");
                p = newPath;
            }
            return p;
        }
    };

    public <M extends Model> void fillDefaultsForReferenceFields(M record,Class<M> modelClass){
        List<ControllerInfo> controllerElements = new ArrayList<ControllerInfo>(getControllerElements());
        Collections.reverse(controllerElements);
        
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        for (Method referredModelGetter: reflector.getReferredModelGetters()){
            @SuppressWarnings("unchecked")
            Class<? extends Model> referredModelClass = (Class<? extends Model>)referredModelGetter.getReturnType();
            String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
            if (!reflector.isFieldSettable(referredModelIdFieldName) || reflector.isHouseKeepingField(referredModelIdFieldName) || reflector.getColumnDescriptor(referredModelIdFieldName).isNullable() ){
                continue;
            }
            Method referredModelIdSetter =  reflector.getFieldSetter(referredModelIdFieldName);
            Method referredModelIdGetter =  reflector.getFieldGetter(referredModelIdFieldName);
            
            try {
                Number oldValue = ((Number) referredModelIdGetter.invoke(record));
                if (!Database.getJdbcTypeHelper(reflector.getPool()).isVoid(oldValue)){
                    continue;
                }
                Long valueToSet = null;

                Iterator<ControllerInfo> miIter = controllerElements.iterator();
                if (miIter.hasNext()){
                    miIter.next();
                    //Last model was self so ignore the first one now as model Elements is already reversed.
                }
                while (miIter.hasNext()){
                    ControllerInfo mi = miIter.next();
                    if (mi.getId() == null){
                        continue;
                    }
                    if (mi.getModelClass() == null){
                        continue;
                    }
                    ModelReflector<? extends Model> ref = ModelReflector.instance(mi.getModelClass());
                    if (ref.reflects(referredModelClass)){
                        try {
                            Model referredModel = Database.getTable(referredModelClass).get(mi.getId());
                            if (referredModel.isAccessibleBy(getSessionUser(),referredModelClass)){
                                valueToSet = mi.getId();
                                break;
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (valueToSet == null){
                    List<Long> idoptions = null ;
    
                    PARTICIPANT participant = reflector.getAnnotation(referredModelIdGetter, PARTICIPANT.class);
                    if (participant != null && participant.defaultable()){
                        idoptions = getSessionUser().getParticipationOptions(modelClass,record).get(participant.value()).get(referredModelIdFieldName);
                    }
                            
                    if (idoptions != null && !idoptions.isEmpty()){
                        if (idoptions.size() == 1){
                            valueToSet = idoptions.get(0);
                        }else if (idoptions.size() == 2 && idoptions.contains(null)){
                            for (Long i:idoptions){
                                if (i != null){
                                    valueToSet = i;
                                }
                            }
                        }
                    }
                }
                if (valueToSet != null){
                    Model referredModel = Database.getTable(referredModelClass).get(valueToSet);
                    if (referredModel.isAccessibleBy(getSessionUser(),referredModelClass)){
                        referredModelIdSetter.invoke(record,valueToSet);
                    }
                }
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public void addMessage(StatusType type, String message){
        HttpSession session = getSession();

        if (session == null){
            session = getRequest().getSession(true);
            setSession(session);
        }
        @SuppressWarnings("unchecked")
        List<String> existing = (List<String>) session.getAttribute(type.getSessionKey());
        if (existing == null){
            existing = new SequenceSet<String>();
            session.setAttribute(type.getSessionKey(), existing);
        }
        if (!ObjectUtil.isVoid(message)){
            existing.add(message);
        }
    }
    public List<String> getMessages(StatusType type){
        SequenceSet<String> ret = new SequenceSet<String>(); 
        if (getSession() == null){
            return ret;
        }
        HttpSession session = getSession();
        @SuppressWarnings("unchecked")
        List<String> existing = (List<String>) session.getAttribute(type.getSessionKey());
        if (existing != null){
            ret.addAll(existing);
            existing.clear();
        }
        return ret;
        
    }
    @Override
    public void addErrorMessage(String msg) {
        addMessage(StatusType.ERROR, msg);
    }
    @Override
    public void addInfoMessage(String msg) {
        addMessage(StatusType.INFO, msg);
        
    }
    @Override
    public List<String> getErrorMessages() {
        return getMessages(StatusType.ERROR);
    }
    @Override
    public List<String> getInfoMessages() {
        return getMessages(StatusType.INFO);
    }
    
    public boolean isForwardedRequest(){
        return !ObjectUtil.isVoid(getRequest().getAttribute("javax.servlet.forward.request_uri"));
    }

    public String getHeader(String key){
        String value = getRequest().getHeader("X-"+key);

        if (ObjectUtil.isVoid(value)){
            value = getRequest().getHeader(key);
        }

        if (ObjectUtil.isVoid(value)){
            value = getRequest().getParameter(key);
        }
        return value;
    }
}
