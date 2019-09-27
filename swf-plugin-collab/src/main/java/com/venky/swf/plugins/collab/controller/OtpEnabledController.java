package com.venky.swf.plugins.collab.controller;

import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.agents.SendOtp;
import com.venky.swf.plugins.collab.db.model.user.OtpEnabled;
import com.venky.swf.views.View;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public abstract class OtpEnabledController<T extends Model & OtpEnabled> extends ModelController<T> {
    public OtpEnabledController(Path path) {
        super(path);
    }

    public View sendOtp(long id, String otpField){
        T otpEnabledModel = Database.getTable(getModelClass()).get(id);
        _sendOtp(otpEnabledModel,otpField);
        return getIntegrationAdaptor().createStatusResponse(getPath(),null,"Otp Sent to your " + StringUtil.camelize(otpField) + " :" +
                getReflector().get(otpEnabledModel,otpField));

    }
    public <F> View validateOtp(long id, String otpField) throws  Exception{
        T otpEnabledModel = Database.getTable(getModelClass()).get(id);
        _validateOtp(otpEnabledModel,otpField);
        return getIntegrationAdaptor().createResponse(getPath(),otpEnabledModel, getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()));
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
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Only POST Supported");
        }
        if (getIntegrationAdaptor() == null){
            throw new RuntimeException("Content-Type must be application/xml or application/json");
        }

        FormatHelper<F> helper = FormatHelper.instance(getPath().getProtocol(),request.getInputStream());
        String otp = helper.getAttribute("Otp");

        otpEnabledModel.validateOtp(otp);
    }
}
