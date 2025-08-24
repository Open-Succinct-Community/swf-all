package com.venky.swf.plugins.collab.controller;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.agents.SendOtp;
import com.venky.swf.plugins.collab.db.model.user.OtpEnabled;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.model.ModelEditView;
import org.eclipse.jetty.server.Request;


import java.util.Arrays;
import java.util.List;

public abstract class OtpEnabledController<T extends Model & OtpEnabled> extends ModelController<T> {
    public OtpEnabledController(Path path) {
        super(path);
    }

    public View sendOtp(long id, String otpField){
        T otpEnabledModel = Database.getTable(getModelClass()).lock(id);
        _sendOtp(otpEnabledModel,otpField);
        if (getIntegrationAdaptor() == null){
            getPath().addInfoMessage("Otp Sent to "  +
                    getReflector().get(otpEnabledModel,otpField));
            return back();
        }else {
            return getIntegrationAdaptor().createStatusResponse(getPath(), null, "Otp Sent to " +
                    getReflector().get(otpEnabledModel, otpField));
        }

    }
    public View validateOtp(long id, String otpField) throws  Exception{
        T otpEnabledModel = Database.getTable(getModelClass()).lock(id);
        if (getPath().getRequest().getMethod().equalsIgnoreCase("GET")){
            return dashboard(new ModelEditView<T>(getPath(),new String[]{"ID","PHONE_NUMBER", "EMAIL" ,"OTP"},otpEnabledModel,"validateOtp"){
                @Override
                public SequenceSet<HotLink> getTabLinks() {
                    return new SequenceSet<>();
                }
            });
        }else {
            _validateOtp(otpEnabledModel,otpField);
            return otpValidationComplete(otpEnabledModel);
        }
    }
    public View otpValidationComplete(T otpEnabledModel){
        return getIntegrationAdaptor().createResponse(getPath(),otpEnabledModel, getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()));
    }
    public View validateOtp(){
        IntegrationAdaptor<T,?> integrationAdaptor = getIntegrationAdaptor();
        if (integrationAdaptor == null){
            return performPostAction(new Action<T>() {
                @Override
                public View noAction(T t) {
                    return noActionView(t);
                }

                @Override
                public void act(T t) {
                    t.validateOtp();
                }

                @Override
                public <C extends Model> void actOnChild(T parent, Class<C> childModelClass, Model c) {

                }

                @Override
                public View error(T t ,boolean newRecord) {
                    HtmlView errorView =  new ModelEditView<T>(getPath(),new String[]{"OTP"},t,"validateOtp");
                    return errorView;
                }
            });
        }else {
            List<T> otpEnabledModels = integrationAdaptor.readRequest(getPath());
            for (T otpEnabledModel: otpEnabledModels){
                otpEnabledModel.validateOtp();
            }
            return integrationAdaptor.createResponse(getPath(),otpEnabledModels);
        }
    }

    protected void _sendOtp(long id, String otpField){
        T otpEnabledModel = Database.getTable(getModelClass()).get(id);
        _sendOtp(otpEnabledModel,otpField);
    }
    protected void _sendOtp(T otpEnabledModel, String otpField){
        if (otpEnabledModel == null){
            throw new RuntimeException( StringUtil.camelize(otpField)  + " not found " );
        }
        TaskManager.instance().executeAsync(new SendOtp(otpEnabledModel),false);
    }
    protected <F> void _validateOtp(long id, String otpField) throws  Exception {
        T otpEnabledModel = Database.getTable(getModelClass()).get(id);
        _validateOtp(otpEnabledModel,otpField);
    }
    protected <F> void _validateOtp(T otpEnabledModel, String otpField) throws  Exception{
        if (otpEnabledModel == null){
            throw new RuntimeException( "Could not send otp to " + StringUtil.camelize(otpField));
        }
        Request request = getPath().getRequest();


        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Only POST Supported");
        }
        if (getIntegrationAdaptor() == null){
            throw new RuntimeException("Content-Type must be application/xml or application/json");
        }

        FormatHelper<F> helper = FormatHelper.instance(getPath().getProtocol(),getPath().getInputStream());
        String otp = helper.getAttribute("Otp");

        otpEnabledModel.validateOtp(otp);
    }

}
