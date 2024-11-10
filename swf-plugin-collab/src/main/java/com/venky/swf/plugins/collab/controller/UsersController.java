package com.venky.swf.plugins.collab.controller;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.db.model.io.ModelIO;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import com.venky.swf.plugins.collab.db.model.participants.EventHandler;
import com.venky.swf.plugins.collab.db.model.participants.WhiteListIp;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.participants.admin.FacilityCategory;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Paragraph;
import com.venky.swf.views.login.LoginView.LoginContext;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UsersController extends com.venky.swf.controller.UsersController {
    public UsersController(Path path) {
        super(path);
    }
    public View current() {
        return new RedirectorView(getPath(),"show/" + getPath().getSessionUserId());
    }

    public View addPhone(String phoneNumber){
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        User user = users.get(0).getRawRecord().getAsProxy(User.class);
        if (user.getRawRecord().isNewRecord()){
            throw new RuntimeException("User not registered yet.");
        }

        String sanitizePhoneNumber  = Phone.sanitizePhoneNumber(phoneNumber);
        List<UserPhone> userPhones = user.getUserPhones().stream().filter(up->ObjectUtil.equals(up.getPhoneNumber(),sanitizePhoneNumber)).collect(Collectors.toList());
        if (userPhones.isEmpty()){
            UserPhone userPhone = Database.getTable(UserPhone.class).newRecord();
            userPhone.setPhoneNumber(phoneNumber);
            userPhone.setUserId(user.getId());
            userPhone.save();
        }

        return getIntegrationAdaptor().createResponse(getPath(),user, getIncludedModelFields().get(User.class), new HashSet<>(),getIncludedModelFields());
    }
    public View addEmail(String emailId){
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        User user = users.get(0).getRawRecord().getAsProxy(User.class);
        if (user.getRawRecord().isNewRecord()){
            throw new RuntimeException("User not registered yet.");
        }
        List<UserEmail> userEmails = user.getUserEmails().stream().filter(up->ObjectUtil.equals(up.getEmail(),emailId)).map(ue->ue.getRawRecord().getAsProxy(UserEmail.class)).collect(Collectors.toList());
        if (userEmails.isEmpty()){
            UserEmail userEmail = Database.getTable(UserEmail.class).newRecord();
            userEmail.setEmail(emailId);
            userEmail.setUserId(user.getId());
            userEmail.save();
        }

        return getIntegrationAdaptor().createResponse(getPath(),user, getIncludedModelFields().get(User.class), new HashSet<>(),getIncludedModelFields());
    }

    public View sendMail(User user, String subject, String text){
        user.getRawRecord().getAsProxy(User.class).sendMail(subject,text);

        if (getIntegrationAdaptor() != null) {
            return getIntegrationAdaptor().createStatusResponse(getPath(), null, "Mail sent with instructions to set password");
        }else {
            if (getPath().getSessionUserId() == null) {
                HtmlView view = createLoginView(LoginContext.PASSWORD_RESET);
                view.setStatus(StatusType.INFO, "Message sent with instructions to set password");
                return view;
            }else {
                getPath().addInfoMessage("Message sent with instructions to set password");
                return back();
            }
        }
    }

    public View sendInviteMail(long id) {
        User user = Database.getTable(User.class).get(id);
        return sendPasswordResetEmail(user, "before.send.email.invite");
    }
    protected View sendPasswordResetEmail(User user, String extensionPointName){
        if (ObjectUtil.isVoid(user.getApiKey())){
            user.generateApiKey(true);
        }

        Link link = new Link();
        link.setUrl(Config.instance().getProperty("swf.password.reset.link",
                Config.instance().getServerBaseUrl() + "/users/reset_password")+"?ApiKey=" + user.getApiKey());
        link.setText("here");


        Paragraph p = new Paragraph();
        p.setText("To set your password , click ");
        p.addControl(link);

        Html html = new Html();
        Body body = new Body();
        html.addControl(body);
        body.addControl(p);

        Map<String,Object> context  = new HashMap<>(){{
            put("SUBJECT","Password reset request received");
            put("TEXT",html);
            put("RESET_LINK",link);
            put("USER", user);
        }};
        Registry.instance().callExtensions(extensionPointName == null ? "before.send.password.reset" : extensionPointName,context);



        return sendMail(user,(String)context.get("SUBJECT"),String.valueOf(context.get("TEXT"))); //Reset link is auto substituted;
    }


    @Override
    @RequireLogin(value = false)
    public View forgot_password(String userName){
        com.venky.swf.db.model.User user = getPath().getUser("NAME",userName);


        if (user == null){
            Select select = new Select().from(UserEmail.class);
            List<UserEmail> emails = select.where(new Expression(select.getPool(), Conjunction.AND)
                    .add(new Expression(select.getPool(),"EMAIL", Operator.EQ,userName))
                    .add(new Expression(select.getPool(), "VALIDATED",Operator.EQ,true))).execute();

            if (emails.size() == 1) {
                user = emails.get(0).getUser();
            }
        }
        if (user == null){
            throw new RuntimeException("Email not registered");
        }

        return sendPasswordResetEmail(user.getRawRecord().getAsProxy(User.class),"before.send.password.reset" );

    }

    @Override
    protected String[] getIncludedFields() {
        Map<Class<? extends Model>, List<String>> map  = getIncludedModelFields();
        if (map.containsKey(User.class)){
            return map.get(User.class).toArray(new String[]{});
        }else {
            return null;
        }
    }


    public List<String> getExistingModelFields(Map<Class<? extends Model>,List<String>> map , Class<? extends Model> modelClass){
        List<String> existing = new SequenceSet<>();
        if (map != null) {
            ModelReflector.instance(modelClass).getModelClasses().forEach(mc -> {
                List<String> e = map.get(mc);
                if (e != null) {
                    existing.addAll(e);
                }
            });
        }
        return existing;
    }
    protected void addToIncludedModelFieldsMap(Map<Class<? extends Model>,List<String>> map, Class<? extends Model> clazz , List<String> excludedFields){
        List<String> fields = ModelReflector.instance(clazz).getVisibleFields(List.of("ID"));
        List<String> oldFields = getExistingModelFields(map,clazz);

        Map<Class<? extends Model>,List<String>> requestedFieldsMap  = getIncludedModelFieldsFromRequest();
        List<String> requestedFields = getExistingModelFields(requestedFieldsMap,clazz);


        SequenceSet<String> finalFields = new SequenceSet<>();
        finalFields.addAll(fields);
        finalFields.addAll(oldFields);
        excludedFields.forEach(finalFields::remove);
        finalFields.addAll(requestedFields);// Ensure Requested is always added
        map.put(clazz,finalFields);
    }
    public void removeChildModelClasses(Map<Class<? extends Model>,List<Class<? extends Model>>> map, Class<? extends Model> parentClass, Class<? extends Model> childClass){
        ModelReflector.instance(parentClass).getModelClasses().forEach(pc->{
            ModelReflector.instance(childClass).getModelClasses().forEach(cc->{
                map.get(pc).remove(cc);
            });
        });
    }
    public void removeModelFields(Map<Class<? extends Model>,List<String>> map , Class<? extends Model> modelClass, List<String> fields){
        ModelReflector.instance(modelClass).getModelClasses().forEach(mc->{
            List<String> existing = map.get(mc);
            if (existing != null) {
                existing.removeAll(fields);
            }
        });

    }

    @RequireLogin(false)
    @SuppressWarnings("unchecked")
    public View hasPassword() throws Exception {
        ensureIntegrationMethod(HttpMethod.POST);
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        JSONObject out = new JSONObject();
        if (users.size() == 1){
            User user = users.get(0).getRawRecord().getAsProxy(User.class);
            boolean newRecord = user.getRawRecord().isNewRecord();
            out.put("Registered",!newRecord);
            if (!newRecord){
                out.put("PasswordSet", !ObjectUtil.isVoid(user.getPassword()));
            }

        }

        return new BytesView(getPath(), out.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
    }

    /* Olde One Refactored
    @Override
    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>,List<String>> map = super.getIncludedModelFields();
        if( getReturnIntegrationAdaptor() == null ){
            return map ;
        }
        if (!map.containsKey(User.class)) {
            map.put(User.class, new ArrayList<>(List.of("ID", "NAME")));
        }
        if (!map.containsKey(UserPhone.class)) {
            map.put(UserPhone.class, new ArrayList<>(List.of("ID", "PHONE_NUMBER", "VALIDATED")));
        }
        if (!map.containsKey(UserEmail.class)){
            map.put(UserEmail.class, new ArrayList<>(List.of("ID", "EMAIL", "VALIDATED")));
        }
        return map;
    }
    */


    @Override
    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>, List<String>> map = super.getIncludedModelFields();
        if (getReturnIntegrationAdaptor() == null) {
            return map;
        }

        addToIncludedModelFieldsMap(map, User.class, List.of("COMPANY_ID",  "COUNTRY_ID", "STATE_ID"));
        addToIncludedModelFieldsMap(map, UserRole.class, List.of("USER_ID"));

        addToIncludedModelFieldsMap(map, UserPhone.class, List.of("USER_ID"));
        addToIncludedModelFieldsMap(map, UserEmail.class, List.of("USER_ID",  "COMPANY_ID"));
        addToIncludedModelFieldsMap(map, Application.class, List.of( "COMPANY_ID", "ADMIN_ID"));
        addToIncludedModelFieldsMap(map, Event.class, Collections.emptyList());
        addToIncludedModelFieldsMap(map, ApplicationPublicKey.class, List.of( "APPLICATION_ID"));
        addToIncludedModelFieldsMap(map, EndPoint.class, List.of( "APPLICATION_ID"));
        addToIncludedModelFieldsMap(map, EventHandler.class, List.of( "ADMIN_ID", "APPLICATION_ID", "END_POINT_ID"));
        addToIncludedModelFieldsMap(map, WhiteListIp.class, List.of( "APPLICATION_ID"));
        addToIncludedModelFieldsMap(map, OpenApi.class,  List.of());
        addToIncludedModelFieldsMap(map, Country.class, List.of());
        addToIncludedModelFieldsMap(map, State.class, List.of());
        addToIncludedModelFieldsMap(map, City.class, List.of());
        addToIncludedModelFieldsMap(map, PinCode.class, List.of( "STATE_ID", "CITY_ID"));
        addToIncludedModelFieldsMap(map, Facility.class, List.of( "COMPANY_ID"));
        addToIncludedModelFieldsMap(map, Company.class, List.of( "LOGO"));

        return map;
    }

    @Override
    protected Map<Class<? extends Model>, List<Class<? extends Model>>> getConsideredChildModels() {
        Map<Class<? extends Model>, List<Class<? extends Model>>> m = super.getConsideredChildModels();
        removeChildModelClasses(m,Company.class,User.class);
        removeChildModelClasses(m,Company.class,UserEmail.class);
        removeChildModelClasses(m,Application.class, EventHandler.class);
        m.get(Facility.class).add(FacilityCategory.class);
        m.get(Company.class).add(Facility.class);


        return m;
    }



}
