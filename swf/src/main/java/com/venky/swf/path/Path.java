/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.path;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.venky.cache.Cache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.Unrestricted;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.model.CONTROLLER;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model._Identifiable;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views._IView;

/**
 *
 * @author venky
 */
public class Path implements _IPath{
	private static Logger logger; 
	
	
	static {
		logger = Logger.getLogger(Path.class.getName());
		logger.info("Loaded by " + Path.class.getClassLoader());
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
    	formFields = new HashMap<String, Object>();
    	HttpServletRequest request = getRequest();
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        if (isMultiPart){
        	FileItemFactory factory = new DiskFileItemFactory(1024*1024*128, new File(System.getProperty("java.io.tmpdir")));
        	ServletFileUpload fu = new ServletFileUpload(factory);
        	try {
				List<FileItem> fis = fu.parseRequest(request);
				for (FileItem fi:fis){
					if (fi.isFormField()){
						if (!formFields.containsKey(fi.getFieldName())){
							formFields.put(fi.getFieldName(), fi.getString());
						}
					}else {
						byte[] content = StringUtil.readBytes(fi.getInputStream());
						if (content == null || content.length == 0){
							content = null;
						}else {
							formFields.put(fi.getFieldName() + "_CONTENT_TYPE", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fi.getName()));
							formFields.put(fi.getFieldName() + "_CONTENT_NAME", fi.getName());
							formFields.put(fi.getFieldName() + "_CONTENT_SIZE", fi.getSize());
						}
						formFields.put(fi.getFieldName(), content == null ? null : new ByteArrayInputStream(content));
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
            	formFields.put(name,request.getParameter(name));
        	}
        }
        return formFields;

    }
    public User getSessionUser(){
    	if (getSession() == null){
    		return null;
    	}
    	_Identifiable user = null; 
    	try {
    		user =  (_Identifiable)getSession().getAttribute("user");
    		return (User)user;
    	}catch (ClassCastException ex){
    		boolean isModel = false;
    		boolean isUser = false; 
    		for (Class<?> infc:user.getClass().getInterfaces()){
    			if (infc.getName().equals(Model.class.getName())){
    				isModel = true;
    			}
    			if (infc.getName().equals(User.class.getName())){
    				isUser = true;
    			}
    		}
    		if (isModel && isUser && user.getClass().getClassLoader() != getClass().getClassLoader()){
				User reloadedUser = Database.getTable(User.class).get(user.getId()); // Reload the user with new class loader.
    			getSession().setAttribute("user", reloadedUser);
    			return reloadedUser;
    		}else {
    			return null;
    		}
    	}
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

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
    protected void logHeaders(){
    	if (request != null){
	        List<String> headers = new ArrayList<String>();
	        Enumeration<String> names = request.getHeaderNames();
	        while(names.hasMoreElements()){
	        	headers.add(names.nextElement());
	        }
	        logger.info("Request Headers:" + headers.toString());
    	}
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }
    
    public Path(String target) {
        this.target = target;

        StringTokenizer stok = new StringTokenizer(target, "/");
        boolean isResource = false;
        while (stok.hasMoreTokens()) {
        	String token = stok.nextToken();
        	if (pathelements.isEmpty()){
            	pathelements.add(token);
            	if (token.equals("resources")){
            		isResource = true;
            	}
        	}else if (isResource){
        		if (pathelements.size() == 1){
        			pathelements.add("/"+token);
        		}else {
            		String sofar = pathelements.remove(1);
            		pathelements.add(sofar+"/"+token);
        		}
        	}else {
        		pathelements.add(token);
        	}
        }
        
        int pathElementSize = pathelements.size();
        for (int i = 0 ; !isResource && i < pathElementSize ; i++){
        	String token = pathelements.get(i);
        	Class<? extends Model> modelClass = getModelClass(token);
        	if (modelClass != null){
        		ModelInfo info = new ModelInfo(modelClass);
        		modelElements.add(info);
        		if (i <pathElementSize -1){
	        		info.setAction(pathelements.get(i+1));
	        		i+= 1;
        		}
        		try {
        			if (i < pathElementSize - 1){
	        			info.setId(Integer.valueOf(pathelements.get(i+1)));
	        			i+=1;
        			}
        		}catch (NumberFormatException ex){
        			//
        		}
        	}
    	}
        loadControllerClassName();
    }
    
    public static class ModelInfo{
    	private ModelReflector<? extends Model> reflector;
    	private Integer id;
    	private String action = "index";
    	public ModelInfo(Class<? extends Model> modelClass){
    		this.reflector = ModelReflector.instance(modelClass);
    	}
    	public ModelReflector<? extends Model> getReflector(){
    		return reflector;
    	}
    	
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getAction() {
			return action;
		}
		public void setAction(String action) {
			this.action = action;
		}
		
    }
    
    private List<ModelInfo> modelElements = new ArrayList<Path.ModelInfo>();
    
    public List<ModelInfo> getModelElements(){
    	return modelElements;
    }
    

    public String getTarget() {
        return target;
    }

    private void loadControllerClassName(){
        if (controllerClassName != null){
            return;
        }
        
        boolean controllerFound = false;
        for (int i = pathelements.size() - 1; i >= 0 && !controllerFound; i--) {
            String pe = pathelements.get(i);
            
            for (String controllerPackageRoot: Config.instance().getControllerPackageRoots()){
                String clazzName = controllerPackageRoot + "." + camelize(pe) + "Controller";
                if (getClass(clazzName) != null) {
                    controllerClassName = clazzName;
                    controllerPathIndex = i ;
                    controllerFound = true;
                    break;
                }
            }
            if (!controllerFound){
	            Class<? extends Model> modelClass = getModelClass(pe);
	            if (modelClass != null){
	            	ModelReflector<?> ref = ModelReflector.instance(modelClass);
	            	CONTROLLER controller = ref.getAnnotation(CONTROLLER.class);
	            	if (controller != null){
	            		controllerClassName = controller.value();
	            	}
	            	if (ObjectUtil.isVoid(controllerClassName)){
	                    controllerClassName = ModelController.class.getName();
	            	}
	                controllerPathIndex = i ;
	                controllerFound = true;
	            }
            }
            if (controllerFound){
            	break;
            }
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
    	StringBuilder backTarget = new StringBuilder();
    	if (controllerPathIndex > 0 && controllerPathIndex < pathelements.size()) {
        	for (int i = 0 ; i < controllerPathIndex ; i ++  ){
        		backTarget.append("/");
        		backTarget.append(pathelements.get(i));
        	}
    	}
    	if (backTarget.length() == 0){
    		backTarget.setLength(0);
    		backTarget.append(controllerPath()).append("/index");
    	}
    	return backTarget.toString();
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
        String tableName = Table.tableName(StringUtil.singularize(camelize(pathElement)));
		Table<M> table = Database.getTable(tableName);
        return table;
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
    	return getSessionUser() != null ; 
    }
    
    public boolean isSecuredAction(Method m){
    	return !m.isAnnotationPresent(Unrestricted.class);
    }
    
    private void createUserSession(User user,boolean autoInvalidate){
    	HttpSession session = getRequest().getSession(true);
    	session.setAttribute("user", user);
    	session.setAttribute("autoInvalidate", autoInvalidate);
    	setSession(session);
    }
    public boolean isRequestAuthenticated(){
    	if (isUserLoggedOn()){
    		return true;
    	}
    	User user = null;
    	String apiKey = getRequest().getHeader("ApiKey");
    	if (!ObjectUtil.isVoid(apiKey)){
	        user = getUser("api_key",apiKey);
        }

        if (user == null){
        	if (getRequest().getMethod().equalsIgnoreCase("POST")){
	        	String username = getRequest().getParameter("name");
	            if (!ObjectUtil.isVoid(username)){
	            	logger.fine("Logging in " + username);
	            	user = getUser("name",username);
	            	logger.fine("User is valid ? " + (user != null));
	            	
	                String password = getRequest().getParameter("password");
	            	if (user != null && user.authenticate(password)){
	            		createUserSession(user,false);
	            	}else {
	            		logger.fine("Authentication Failed");
	            	}
	            }
        	}
        }else {
        	createUserSession(user,true);
        }
        
        return isUserLoggedOn();
    }
    
    public MimeType getProtocol(){
    	String apiprotocol = getRequest().getHeader("ApiProtocol");
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
    protected User getUser(String fieldName, String fieldValue){
        Select q = new Select().from(User.class);
        String nameColumn = ModelReflector.instance(User.class).getColumnDescriptor(fieldName).getName();
        q.where(new Expression(nameColumn,Operator.EQ,new BindVariable(fieldValue)));
        
		List<? extends User> users  = q.execute(User.class);
        if (users.size() == 1){
        	return users.get(0);
        }
        return null;
    }
    

    public _IView invoke() throws AccessDeniedException{
    	MultiException ex = null;
    	List<Method> methods = getActionMethods(action(), parameter());
    	for (Method m :methods){
        	Timer timer = Timer.startTimer(); 
        	try {
            	boolean securedAction = isSecuredAction(m) ;
            	if (securedAction){
            		if (!isRequestAuthenticated()){
        				return new RedirectorView(this,"","login");
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
                    }
            	}catch(Exception e){
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
    	throw new RuntimeException("Donot know how to invoke controller action" + getTarget()) ;
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
    
    public List<Method> getActionMethods(final String actionPathElement,final String parameterPathElement){
    	List<Method> methods = getControllerReflector().getMethods(new MethodMatcher() {
			public boolean matches(Method method) {
				return method.getName().equals(actionPathElement) && View.class.isAssignableFrom(method.getReturnType()) && method.getParameterTypes().length <= 1;
			}
		});
    	final int targetParameterLength = ObjectUtil.isVoid(parameterPathElement)? 0 : 1;
    	boolean parameterIsNumeric = false;

		if (targetParameterLength == 1) {
			try {
				Double.parseDouble(parameterPathElement);
				parameterIsNumeric = true;
			}catch (NumberFormatException nfex){
				// 
			}
		}
		final Class<?> targetParameterType = targetParameterLength == 0 ? null : (parameterIsNumeric ? int.class  : String.class);
		
    	Collections.sort(methods,new Comparator<Method>(){
			public int compare(Method o1, Method o2) {
				int ret = 0 ;
				int s1 = 0 ; int s2 = 0 ; 
				s1 = Math.abs(o1.getParameterTypes().length - targetParameterLength);
				s2 = Math.abs(o2.getParameterTypes().length - targetParameterLength) ;
				ret = s1 - s2; 
				if (ret == 0 && o1.getParameterTypes().length == 1){
					s1 = o1.getParameterTypes()[0].equals(targetParameterType) ? 0 : 1;
					s2 = o2.getParameterTypes()[0].equals(targetParameterType) ? 0 : 1;
					ret = s1 - s2;
				}
				return ret;
			}
    		
    	});
    	
    	return methods;
    }
    public boolean canAccessControllerAction(String actionPathElement,String parameterPathElement){
    	boolean accessible =  canAccessControllerAction(getSessionUser(), controllerPathElement(), actionPathElement, parameterPathElement);
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

    public static boolean canAccessControllerAction(User user,String controllerPathElement,String actionPathElement,String parameterPathElement){
    	try {
    		ensureControllerActionAccess(user,controllerPathElement,actionPathElement,parameterPathElement);
    	}catch (AccessDeniedException ex){
    		return false;
    	}
    	return true;
    }
    
    private void ensureControllerActionAccess() throws AccessDeniedException{
    	ensureControllerActionAccess(getSessionUser(),controllerPathElement(),action(),parameter()); 
    }
    private static void ensureControllerActionAccess(User user,String controllerPathElement,String actionPathElement , String parameterPathElement) throws AccessDeniedException{
    	Registry.instance().callExtensions(ALLOW_CONTROLLER_ACTION, user, controllerPathElement,actionPathElement,parameterPathElement);
    }
    

    @SuppressWarnings("unchecked")
	public <T extends Controller> Class<T> getControllerClass() {
        return (Class<T>) getClass(getControllerClassName());
    }

    private Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
    private String getControllerClassName() {
        return controllerClassName;
    }

    private static String camelize(String s) {
        return StringUtil.camelize(s);
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
    	Path path = new Path(relPath);
    	path.setRequest(getRequest());
    	path.setResponse(getResponse());
    	path.setSession(getSession());
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
    	Expression where = new Expression(Conjunction.AND);
		Map<String, List<Method>> referredModelGetterMap = new HashMap<String, List<Method>>();
		ModelReflector<? extends Model> reflector = ModelReflector.instance(getModelClass());
		
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

		List<ModelInfo> modelElements = getModelElements();

		for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){ // The last model is self.
    		ModelInfo mi = miIter.next();
    		if(!miIter.hasNext()){
    			//last model is self.
    			break;
    		}
    		List<Method> referredModelGetters = getReferredModelGetters(referredModelGetterMap, mi.getReflector().getTableName());
    		
    		if (referredModelGetters.isEmpty() || mi.getId() == null){
    			continue;
    		}
    		
    		Expression referredModelWhere = new Expression(Conjunction.AND);
    		Expression referredModelWhereChoices = new Expression(Conjunction.OR);

    		ModelReflector<?> referredModelReflector = mi.getReflector();
	    	for (Method childGetter : referredModelReflector.getChildGetters()){
	    		Class<? extends Model> childModelClass = referredModelReflector.getChildModelClass(childGetter);
	    		if (reflector.reflects(childModelClass)){
	            	CONNECTED_VIA join = referredModelReflector.getAnnotation(childGetter,CONNECTED_VIA.class);
	            	if (join == null){
	            		for (Method referredModelGetter: referredModelGetters){ 
    	        	    	String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
    	        	    	String referredModelIdColumnName = reflector.getColumnDescriptor(referredModelIdFieldName).getName();

    	        	    	referredModelWhereChoices.add(new Expression(referredModelIdColumnName,Operator.EQ,new BindVariable(mi.getId())));
	            		}
	            	}else {
	            		String referredModelIdColumnName = join.value();
	            		referredModelWhereChoices.add(new Expression(referredModelIdColumnName,Operator.EQ,new BindVariable(mi.getId())));
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
		
		Cache<String,Map<String,List<Integer>>> pOptions = getSessionUser().getParticipationOptions(reflector.getModelClass());
		if (pOptions.size() >  0){
			Set<String> fields = new HashSet<String>();
			for (String g: pOptions.keySet()){
				fields.addAll(pOptions.get(g).keySet());
			}
			boolean canFilterInSQL = !DataSecurityFilter.anyFieldIsVirtual(fields, reflector);
			
			if (canFilterInSQL){
				Expression dsw = getSessionUser().getDataSecurityWhereClause(reflector,pOptions);
				if (dsw.getParameterizedSQL().length()> 0){
					where.add(dsw); 
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
			if (ObjectUtil.equals(session.getAttribute("autoInvalidate"),true)){
				invalidateSession();
    		}
		}
	}
}
