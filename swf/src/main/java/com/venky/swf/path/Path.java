/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.path;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.exceptions.UserNotAuthenticatedException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
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
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPart.Part;
import org.eclipse.jetty.http.MultiPartConfig;
import org.eclipse.jetty.http.MultiPartFormData;
import org.eclipse.jetty.http.MultiPartFormData.Parts;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Fields.Field;
import org.eclipse.jetty.util.Promise.Invocable;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;

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
    private Session session = null ;
    private Request request = null ;
    private Response response = null ;
    Callback callback = null;
    
    private Map<String,Object> formFields = null;
    
    public Map<String, Object> getFormFields(){
        if (formFields != null){
            return formFields;
        }
        formFields = new HashMap<String,Object>();
        Map<String,Object> formInput = new HashMap<String, Object>();
        Request request = getRequest();
        String contentType = getHeaders().get("Content-type");
        
        boolean isMultiPart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
        if (isMultiPart){
            String boundary = MultiPart.extractBoundary(headers.get("Content-Type"));
            MultiPartConfig config = new MultiPartConfig.Builder()
                    .location(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")))
                    .maxPartSize(1024 * 1024)
                    .build();
            
            MultiPartFormData.onParts(request, request, contentType, config, new Invocable<Parts>() {
                @Override
                public void succeeded(Parts result) {
                    for (Part part: result){
                        byte[] content;
                        try {
                            content = Content.Source.asByteBuffer(part.getContentSource()).array();
                            if (content.length == 0){
                                content = null;
                            }else {
                                formInput.put(part.getName() + "_CONTENT_TYPE", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(part.getFileName()));
                                formInput.put(part.getName() + "_CONTENT_NAME", part.getName());
                                formInput.put(part.getName() + "_CONTENT_SIZE", part.getLength());
                            }
                            formInput.put(part.getName(), content == null ? null : new ByteArrayInputStream(content));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        
                    }
                }
                
                @Override
                public void failed(Throwable x) {
                    cat.log(Level.WARNING,"Error reading multipart content",x);
                }
            });
        }else {
            try {
                Fields parameters = Request.getParameters(request);
                for  (Field paramenter : parameters){
                    String name = paramenter.getName();
                    if (name.matches("[^\",:]+")) {
                        //To avoid considering json posts as parameters.
                        formInput.put(name, paramenter.getValue());
                    }
                }
            }catch (Exception ex){
                throw new RuntimeException(ex);
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
                
                Map<String, Object> modelAttributes = modelRecords.computeIfAbsent(index, k -> new HashMap<String, Object>());
                modelAttributes.put(fieldName, formInput.get(key));
            }
        }
        
        return formFields;
    }
    public User getSessionUser(){
        Session session = getSession();
        if (session == null){
            return null;
        }
        
        Table<User> USER = Database.getTable(User.class);
        Number id = getSessionUserId();
        if (id != null){
            return USER.get(id.longValue());
        }
        return null;
    }
    public Long getSessionUserId(){
        if (getSession() == null){
            return null;
        }
        Number id = (Number)getSession().getAttribute("user.id");
        if (id != null){
            return id.longValue();
        }
        return null;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Request getRequest() {
        return request;
    }

    public List<HttpCookie> getCookies(){
        return Request.getCookies(getRequest());
    }

    public HttpCookie getCookie(String name){
        Optional<HttpCookie> cookieOptional = getCookies().stream().filter(c->c.getName().equals(name)).findFirst();
        return cookieOptional.orElse(null);
    }

    private ByteArrayInputStream inputStream = null;
    public ByteArrayInputStream getInputStream() {
        try {
            if (inputStream == null) {
                inputStream = new ByteArrayInputStream(Content.Source.asByteBuffer(getRequest()).array()) ;
            }
            inputStream.close();
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }
        return inputStream;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public String getOriginalRequestUrl(){
        return StringUtil.valueOf(request.getHttpURI());
    }
    
    protected void logHeaders(){
        if (request != null){
            List<String> headers = new ArrayList<String>();
            for (HttpField header: request.getHeaders()){
                headers.add(header.getName());
            }
            Config.instance().getLogger(Path.class.getName()).info("Request Headers:" + headers.toString());
        }
    }

    public Response getResponse() {
        return response;
    }
    
    @Override
    public Callback getCallback() {
        return callback;
    }
    
    @Override
    public void setCallback(Callback callBack) {
        this.callback  = callBack;
    }
    
    public void setResponse(Response response) {
        this.response = response;
    }
    public Path  constructNewPath(String target){
        Path p = new Path(target);
        p.setSession(getSession());
        p.setRequest(getRequest());
        p.setResponse(getResponse());
        p.setCallback(getCallback());
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
                if (resourcePath.isEmpty()) {
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
                    InputStream resourceStream = getResourceAsStream(resourcePath.toString());
                    if (resourceStream == null) {
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
    Map<String,InputStream> map = new HashMap<>();
    public ByteArrayInputStream getResourceAsStream(String path){
        InputStream is = map.get(path) ;
        if (is != null){
            return (ByteArrayInputStream) is;
        }
        File dir = new File("./src/main/resources");
        if (dir.isDirectory()) {
            File resource = new File(dir + "/" + path);
            if (resource.exists() && resource.isFile()) {
                try {
                    is = new FileInputStream(new File(dir + "/" + path));
                } catch (Exception ex) {
                    is = null;
                }
            }
        }
        if (is == null) {
            is = getClass().getResourceAsStream(path);
        }
        ByteArrayInputStream bais = null;

        if (is != null){
            if (is instanceof ByteArrayInputStream){
                bais = (ByteArrayInputStream) is;
            }else {
                byte[] bytes = StringUtil.readBytes(is, true);
                bais = new ByteArrayInputStream(bytes);
            }
            map.put(path,bais);
        }
        return (ByteArrayInputStream) map.get(path);
    }

    private void checkPathOverrides(List<String> pathComponents) {
        Registry.instance().callExtensions("swf.before.routing", pathComponents);
    }

    public List<String> getPathElements() {
        return pathelements;
    }
    
    @SuppressWarnings("unchecked")
    private void loadControllerElements(){
        
        for (int i = 0 ; i < pathelements.size() ; i ++ ) {
            String token = pathelements.get(i);
            ControllerInfo lastKnownControllerInfo = controllerElements.isEmpty() ? null : controllerElements.get(controllerElements.size()-1);
            
            String controllerClassName = ControllerCache.instance().get(token);
            
            if (lastKnownControllerInfo != null) {
                if (ObjectUtil.isVoid(lastKnownControllerInfo.getAction())) {
                    List<Method> actions = ControllerReflector.instance((Class<? extends Controller>) lastKnownControllerInfo.getControllerClass()).getActionMethods(token);
                    if (!actions.isEmpty()) {
                        lastKnownControllerInfo.setAction(token);
                    } else {
                        pathelements.add(i, "index");
                        lastKnownControllerInfo.setAction("index");
                    }
                } else if (ObjectUtil.isVoid(lastKnownControllerInfo.getParameter())) {
                    List<Method> actions = ControllerReflector.instance((Class<? extends Controller>) lastKnownControllerInfo.getControllerClass()).getActionMethods(lastKnownControllerInfo.getAction(), 1);
                    if (!actions.isEmpty()) {
                        
                        lastKnownControllerInfo.setParameter(token);
                    } else if (controllerClassName != null) {
                        lastKnownControllerInfo = new ControllerInfo(token, controllerClassName);
                        lastKnownControllerInfo.setControllerPathIndex(i);
                        controllerElements.add(lastKnownControllerInfo);
                    }
                } else if ( controllerClassName != null) {
                    lastKnownControllerInfo = new ControllerInfo(token, controllerClassName);
                    lastKnownControllerInfo.setControllerPathIndex(i);
                    controllerElements.add(lastKnownControllerInfo);
                } else {
                    lastKnownControllerInfo.setParameter(lastKnownControllerInfo.getParameter() + "/" + token);
                }
            } else if (controllerClassName != null){
                lastKnownControllerInfo = new ControllerInfo(token, controllerClassName);
                lastKnownControllerInfo.setControllerPathIndex(i);
                controllerElements.add(lastKnownControllerInfo);
            }
        }
        
        
    }

    @SuppressWarnings("unchecked")
    public Path(String target) {
        this.target = target;
        Config.instance().getLogger(Path.class.getName()).log(Level.INFO,"Api Called:" + target);
        pathelements = parsePathElements(target);
        
        boolean isResource = !pathelements.isEmpty() && pathelements.get(0).equals("resources");
        if (!isResource){
            loadControllerElements();
        }
        loadControllerClassName();
    }

    public boolean isAppAuthenticationRequired() {
        return getProtocol() != MimeType.TEXT_HTML  && Config.instance().getBooleanProperty("swf.application.authentication.required",false);
    }
    public boolean isAppAuthenticated() {
        return getApplication() != null ;
    }
    private Application application = null;
    public Application getApplication(){
        if (application != null){
            return application;
        }
        Map<String,String> headers = getHeaders();
        headers.put("request-target", request.getMethod().toLowerCase() + " " + getTarget());
        headers.putIfAbsent("host",Config.instance().getHostName());
        headers.putIfAbsent("X-ControllerPath",controllerPath());
        headers.putIfAbsent("X-ControllerAction",action());

        //headers.putIfAbsent("X-Real-IP",getRequest().getRemoteAddr());
        application = ApplicationUtil.find(getInputStream(),headers);
        return application;
    }

    public static class ControllerInfo {
        private String action = null; 
        private Object parameter = null; 
        private Class<? extends Model> modelClass = null;
        private Class<?> controllerClass = null;
        private int controllerPathIndex = -1;
        private String controllerName = null;
        public ControllerInfo(String controllerName, String controllerClassName){
            this.controllerName = controllerName;
            this.controllerClass = Path.getClass(controllerClassName);
            this.modelClass = Path.getModelClass(controllerName);
        }
        
        public String getControllerName() {
            return controllerName;
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
            try {
                return Database.getJdbcTypeHelper("").getTypeRef(long.class).getTypeConverter().valueOf(parameter);
            }catch (NumberFormatException ex) {
                return null;
            }
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
            ControllerInfo cInfo = controllerElements.get(controllerElements.size() -1 );
            controllerClassName = cInfo.getControllerClass().getName();
            controllerPathIndex = cInfo.getControllerPathIndex();
        }

        if (controllerClassName == null) {
            controllerClassName = Controller.class.getName();
            controllerPathIndex = -1;
        }
        actionPathIndex = controllerPathIndex + 1 ;
        parameterPathIndex = controllerPathIndex + 2;

        if (parameterPathIndex < pathelements.size() -1 ){
            StringBuilder filePath = new StringBuilder();
            while ( parameterPathIndex < pathelements.size()){
                if (filePath.length() > 0){
                    filePath.append("/");
                }
                filePath.append(pathelements.remove(parameterPathIndex));
            }
            pathelements.add(filePath.toString()); //To absorb remaining  part of the path.
        }

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
                            url = twobackPath.getTarget() + "?_select_tab="+ new ModelAwareness(backPath, null).getLiteral(backPath.getModelClass().getSimpleName());
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
        if (currentUser != null){
            String lat = getHeader( "Lat");
            String lng = getHeader( "Lng");
            TypeConverter<BigDecimal> tc = currentUser.getReflector().getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
            if (!currentUser.getReflector().isVoid(tc.valueOf(lat)) && !currentUser.getReflector().isVoid(tc.valueOf(lng))){
                currentUser.setCurrentLat(tc.valueOf(lat));
                currentUser.setCurrentLng(tc.valueOf(lng));
                Registry.instance().callExtensions(USER_LOCATION_UPDATED_EXTENSION,this,currentUser);
            }
        }
        
        return currentUser != null ; 
    }

    public void createUserSession(User user,boolean autoInvalidate){
        invalidateSession();

        Session session = getRequest().getSession(true);
        if (user != null && !user.isAccountClosed()){
            session.setAttribute("user.id",user.getId());
            Registry.instance().callExtensions(USER_LOGIN_SUCCESS_EXTENSION,this,user);
        }
        session.setAttribute("autoInvalidate", autoInvalidate);
        setSession(session);
        if (Config.instance().getExternalURIScheme().equals("https")) {
            addSameSiteCookieAttribute();
        }
    }
    private void addSameSiteCookieAttribute() {
        Response response = getResponse();
        Collection<String> headers = response.getHeaders().getValuesList("Set-Cookie");
        boolean firstHeader = true;
        for (String header : headers) { // there can be multiple Set-Cookie attributes
            if (firstHeader) {
                response.getHeaders().put("Set-Cookie", String.format("%s; %s", header, "SameSite=None; Secure"));
                firstHeader = false;
                continue;
            }
            response.getHeaders().add("Set-Cookie", String.format("%s; %s", header, "SameSite=None; Secure"));
        }
    }


    public static final String REQUEST_AUTHENTICATOR = "request.authenticator";
    public User login(String username, String password, String password2){
        return login(username,password,password2,true);
    }
    public User login(String username, String password, String password2,boolean save){
        if (ObjectUtil.isVoid(username)){
            throw new RuntimeException("Username is blank.");
        }
        if (ObjectUtil.isVoid(password)){
            throw new RuntimeException("Password is blank");
        }

        boolean isLoginRequest =  getProtocol() == MimeType.TEXT_HTML ? !getFormFields().containsKey("password2") : ObjectUtil.isVoid(password2) ;
        User user = getUser("name",username);
        if (user == null && isLoginRequest){
            throw new RuntimeException("Login failed");
        }else if (isLoginRequest){
            if (!user.authenticate(password)){
                throw new RuntimeException("Login failed");
            }else {
                return user;
            }
        }else if (!ObjectUtil.equals(password,password2)){
            // Signup or reset password request;!
            throw new RuntimeException("Passwords don't match!");
        }else if (user != null && !ObjectUtil.equals(action(),"reset_password")){
            throw new RuntimeException("Username "+ username + " is already registered");
        }else {
            user = Database.getTable(User.class).newRecord();
            user.setName(username);
            user = Database.getTable(User.class).getRefreshed(user);
            if (save){
                user.setPassword(password);
                saveUserInNewTxn(user);
            }
            return user;
        }
    }
    public void saveUserInNewTxn(User user){
        Transaction txn = Database.getInstance().getTransactionManager().createTransaction();
        try {
            user.save();
            txn.commit();
        }catch (Exception ex){
            txn.rollback(ex);
            throw new RuntimeException(ex);
        }
    }

    Set<String> LOGIN_ACTIONS = new HashSet<>(){{
        add("login");
        add("register");
        add("authorize");
    }};
    public <T> boolean isRequestAuthenticated(){
        if (isUserLoggedOn()){
            return true;
        }
        if (isAppAuthenticationRequired()) {
            if (!isAppAuthenticated()){
                throw new AccessDeniedException("Application not authorized");
            }
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
        String hKeepAlive = getHeader("KeepAlive");

        boolean keepAlive = Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().valueOf(hKeepAlive) || (hKeepAlive == null && adaptor == null);

        autoInvalidate = !keepAlive && !ObjectUtil.isVoid(getHeader("ApiKey"));

        Map<String,Object> map = getFormFields();

        String username = StringUtil.valueOf(map.get("name"));
        String password = StringUtil.valueOf(map.get("password"));
        String password2 = StringUtil.valueOf(map.get("password2"));

        if (getRequest().getMethod().equalsIgnoreCase("POST") &&
                ( ( LOGIN_ACTIONS.contains(action()) && (ObjectUtil.isVoid(controllerPathElement()) || ObjectUtil.equals("oauth",controllerPathElement())))
                    || (LOGIN_ACTIONS.contains(controllerPathElement())&& "index".equals(action()))
                )) {
            if (ObjectUtil.isVoid(username) && user != null ){
                // logged in with api key or other means
                username = user.getName();
            }
            if (adaptor == null){
                try {
                    user = login(username, password, password2);
                }catch (Exception ex){
                    addErrorMessage(ex.getMessage());
                    return false;
                }
            }else {
                try {
                    List<User> input = adaptor.readRequest(this);
                    if (input.size() == 1){
                        Database.getInstance().getCache(ModelReflector.instance(User.class)).clear();
                        username = input.get(0).getName();
                        if (ObjectUtil.isVoid(username) && user != null){
                            username = user.getName();
                        }
                        password = input.get(0).getPassword();
                        password2 = input.get(0).getPassword2();
                        user = login(username,password,password2,false);
                        if (user.getRawRecord().isNewRecord()) {
                            //signedup!
                            user.getRawRecord().load(input.get(0).getRawRecord());
                        }
                        saveUserInNewTxn(user);
                    }
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
            if (!ObjectUtil.isVoid(username)){
                Config.instance().getLogger(Path.class.getName()).fine("Logging in " + username);
                //user = getUser("name",username);
                Config.instance().getLogger(Path.class.getName()).fine("User is valid ? " + (user != null));
                if (user != null ){
                    createUserSession(user,autoInvalidate);
                }else {
                    createUserSession(null,true);
                    Config.instance().getLogger(Path.class.getName()).fine("Login Failed");
                    if (adaptor == null) {
                        addErrorMessage("Login Failed");
                    }else {
                        throw new RuntimeException("Login failed");
                    }
                }
            }
        }else if (user != null){
            createUserSession(user, autoInvalidate);
        }

        return isUserLoggedOn();
    }
    public boolean redirectOnException(){
        return getReturnProtocol().equals(MimeType.TEXT_HTML);
    }

    public String getContentType(){
        return getProtocol().toString();
    }
    public MimeType getProtocol(){
        String apiprotocol = getHeader("ApiProtocol"); // This is bc.
        if (ObjectUtil.isVoid(apiprotocol)) {
            apiprotocol = getHeader("content-type");
        }
        return Path.getProtocol(apiprotocol);
    }
    public String getAccept(){
        return getReturnProtocol().toString();
    }
    public MimeType getReturnProtocol(){
        String apiprotocol = getHeader("ApiProtocol"); // This is bc.
        if (ObjectUtil.isVoid(apiprotocol)){
            apiprotocol = getHeader("accept");
            if (ObjectUtil.equals("*/*",apiprotocol)){
                apiprotocol = "";
            }
        }
        if (ObjectUtil.isVoid(apiprotocol)){
            return getProtocol();
        }
        return Path.getProtocol(apiprotocol);
    }
    public static MimeType getProtocol(String apiprotocol){
        if (ObjectUtil.isVoid(apiprotocol)){
            return MimeType.TEXT_HTML;
        }
        for (MimeType mt : new MimeType[]{MimeType.APPLICATION_XML,MimeType.APPLICATION_JSON}){
            if (ObjectUtil.equals(mt.toString(),apiprotocol)){
                return mt;
            }else {
                for (String delimiter : new String[]{",",";"}){
                    StringTokenizer tokenizer = new StringTokenizer(apiprotocol,delimiter);
                    while (tokenizer.hasMoreTokens()){
                        String tapiprotocol = tokenizer.nextToken();
                        if (ObjectUtil.equals(tapiprotocol,mt.toString())){
                            return mt;
                        }
                    }
                }

            }
        }
        return MimeType.TEXT_HTML;
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

    private void setCustomCnameProcessing(){
        String[] hostParams = new String[]{null,null};
        String host = getHeader("Host");
        if (host != null){
            String[] parts = host.split(":");
            for (int i = 0 ; i < Math.min(parts.length,2) ; i ++){
                hostParams[i] = parts[i];
            }
        }
        //Set request based host,port and scheme for this thread.
        Config.instance().setHostName(hostParams[0]);
        Config.instance().setExternalPort(hostParams[1]);
        String extScheme = getHeader("URIScheme");
        if (extScheme == null){
            extScheme = request.getHttpURI().getScheme();
        }
        Config.instance().setExternalURIScheme(extScheme);

        String apiKeyCase = getHeader("ApiKeyCase");
        String apiRootRequired = getHeader("ApiRootRequired");

        Config.instance().setApiKeyCase(apiKeyCase == null ? null : KeyCase.valueOf(apiKeyCase));
        Config.instance().setRootElementNameRequiredForApis(apiRootRequired == null ? null : Database.getJdbcTypeHelper("").
                getTypeRef(boolean.class).getTypeConverter().valueOf(apiRootRequired));
    }

    private final SWFLogger cat = Config.instance().getLogger(getClass().getName());
    public _IView invoke() throws AccessDeniedException{
        setCustomCnameProcessing();

        MultiException ex = null;
        List<Method> methods = getActionMethods(action(), parameter());
        for (Method m :methods){
            Timer timer = cat.startTimer(null,Config.instance().isTimerAdditive()); 
            try {
                boolean securedAction = getControllerReflector().isSecuredActionMethod(m) ;
                if (!isRequestAuthenticated() && securedAction){
                    User guest = getGuestUser();
                    if (guest != null){
                        createUserSession(guest, false);
                    }else  {
                        if (getReturnProtocol() == MimeType.TEXT_HTML){
                            addErrorMessage("Login Failed");
                            return new RedirectorView(this,"","login");
                        }else {
                            throw new UserNotAuthenticatedException();
                        }
                    }
                }
                if (securedAction){
                    ensureControllerActionAccess();
                }
                Controller controller = createController();
                try {
                    View result = controller.getCachedResult();
                    if (result != null){
                        return result;
                    }else if (m.getParameterTypes().length == 0 && parameter() == null){
                        result = (View)m.invoke(controller);
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == String.class && parameter() != null){
                        result = (View)m.invoke(controller, parameter());
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == int.class && parameter() != null){
                        result = (View)m.invoke(controller, Integer.valueOf(parameter()));
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == long.class && parameter() != null){
                        result = (View)m.invoke(controller, Long.valueOf(parameter()));
                    }else {
                        continue;
                    }
                    controller.setCachedResult(result);
                    return result;
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
        List<Method> methods = new ArrayList<>(getControllerReflector().getActionMethods(actionPathElement));
        
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
                            ColumnDescriptor columnDescriptor = reflector.getColumnDescriptor(referredModelIdFieldName);

                            String referredModelIdColumnName = columnDescriptor.getName();
                            reflector.set(partiallyFilledModel,referredModelIdFieldName,controllerInfo.getId());
                            if (!columnDescriptor.isVirtual()) {
                                referredModelWhereChoices.add(new Expression(referredModelReflector.getPool(), referredModelIdColumnName, Operator.EQ, new BindVariable(referredModelReflector.getPool(), controllerInfo.getId())));
                            }
                            /*
                            if (reflector.getColumnDescriptor(referredModelIdFieldName).isNullable()){
                                referredModelWhereChoices.add(new Expression(referredModelReflector.getPool(),referredModelIdColumnName,Operator.EQ));
                            }
                            */
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
                        /*
                        if (reflector.getColumnDescriptor(referredModelIdFieldName).isNullable()){
                            referredModelWhereChoices.add(new Expression(referredModelReflector.getPool(),referredModelIdColumnName,Operator.EQ));
                        }
                        */
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
            if (!reflector.isFieldSettable(referredModelIdFieldName) || reflector.isFieldVirtual(referredModelIdFieldName) || reflector.isHouseKeepingField(referredModelIdFieldName) ){ //|| reflector.getColumnDescriptor(referredModelIdFieldName).isNullable()
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
                    if (participant != null && participant.defaultable() && getSessionUser() != null){
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
        Session session = getSession();

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
        Session session = getSession();
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
        return !ObjectUtil.equals(getRequest().getHttpURI().getPath() , getTarget());
    }

    public String getHeader(String key){
        Map<String,String> map = getHeaders();
        
        String value = map.get("X-"+key);

        if (ObjectUtil.isVoid(value)){
            value = map.get(key);
        }

        return value;
    }

    Map<String,String> headers = null;
    public Map<String,String> getHeaders(){
        if (headers == null){
            headers = new IgnoreCaseMap<>();
            {
                HttpFields fields = getRequest().getHeaders();
                for (HttpField field: fields){
                    String name = field.getName();
                    String value = field.getValue();
                    if (value != null) {
                        headers.put(name, value);
                    }
                }
            }
            if (getRequest().getMethod().equalsIgnoreCase(HttpMethod.GET.toString())){
                try {
                    Fields parameters = Request.getParameters(getRequest());
                    for(Field parameter: parameters){
                        String name = parameter.getName();
                        String value = parameter.getValue();
                        if (value != null && headers.get(name) == null) {
                            headers.put(name, value);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return headers;
    }
    

}
