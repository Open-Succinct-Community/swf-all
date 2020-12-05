package com.venky.swf.plugins.templates.db.model.alerts;

import com.venky.swf.db.table.ModelImpl;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DeviceImpl extends ModelImpl<Device> {
    public DeviceImpl(){
        super();
    }
    public DeviceImpl(Device device){
        super(device);
    }

    public JSONObject getSubscriptionJson(){
        JSONObject jsonObject = new JSONObject();
        Device device = getProxy();
        boolean isDeviceIdJSON = (device.getDeviceId().startsWith("{"));
        if (!isDeviceIdJSON){
            jsonObject.put("client","android");
            jsonObject.put("token",device.getDeviceId());
        }else {
            jsonObject = (JSONObject) JSONValue.parse(device.getDeviceId());
        }
        return jsonObject;
    }
}
