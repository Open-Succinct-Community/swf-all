package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.SequenceMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.routing.Config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class BeforeSaveAddress<M extends Address & Model> extends BeforeModelSaveExtension<M> {

    protected String[] getAddressFields() {
        return Address.getAddressFields();
    }

    protected boolean isAddressVoid(M oAddress) {
        return Address.isAddressVoid(oAddress);
    }

    protected boolean isAddressChanged(M oAddress) {
        return Address.isAddressChanged(oAddress);
    }

    protected boolean isOkToSetLocationAsync() {
        return true;
    }

    @Override
    public void beforeSave(M oAddress) {
        if (!isAddressChanged(oAddress) && !oAddress.getReflector().isVoid(oAddress.getLat()) && !oAddress.getReflector().isVoid(oAddress.getLng())) {
            return;
        }

        if (oAddress.getCityId() != null) {
            oAddress.setStateId(oAddress.getCity().getStateId());
        }
        if (oAddress.getStateId() != null) {
            oAddress.setCountryId(oAddress.getState().getCountryId());
        }
        if (!oAddress.getReflector().isVoid(oAddress.getLat()) && !oAddress.getReflector().isVoid(oAddress.getLng())) {
            if (oAddress.getRawRecord().isFieldDirty("LAT") || oAddress.getRawRecord().isFieldDirty("LNG")) {
                //Set by user or LocationSetterTask
                return;
            }
        }
        if (!isAddressVoid(oAddress)) {
            _IPath path  = Database.getInstance().getContext(_IPath.class.getName());
            Map<String,String> headers = path == null ? new HashMap<>() : path.getHeaders();
            
            LocationSetterTask<M> setterTask = new LocationSetterTask<M>(oAddress,headers);
            if (isOkToSetLocationAsync()) {
                TaskManager.instance().executeAsync(setterTask, false);
            } else {
                setterTask.setLatLng(false);
            }
        }
    }

    public static class LocationSetterTask<M extends Address & Model> implements Task {

        M oAddress = null;
        Map<String, String> params = new IgnoreCaseMap<>();
        public LocationSetterTask(M address,Map<String,String>params){
            this.oAddress = address;
            this.params.putAll(Config.instance().getGeoProviderParams());
            this.params.putAll(params);
        }

        private Set<String> getAddressQueries(M oAddress) {
            List<String> fields = oAddress.getReflector().getFields();
            City city = oAddress.getCity();
            State state = oAddress.getStateId() != null ? oAddress.getState() : ( city != null ? city.getState() : null ) ;
            Country country = oAddress.getCountryId() != null ? oAddress.getCountry() : ( state != null ? state.getCountry() : null );
            PinCode pinCode = oAddress.getPinCode();
            
            SequenceSet<String> addressQueries = new SequenceSet<>();
            
            for (String[] geoCodingFields : Address.getGeoCodingFields()) {
                StringBuilder addressQuery = new StringBuilder();
                for (String f : geoCodingFields) {
                    if (fields.contains(f)) {
                        Object value = oAddress.getReflector().get(oAddress, f);
                        if (!oAddress.getReflector().isVoid(value)){
                            addressQuery.append(StringUtil.valueOf(value).replaceAll("[ ]+", "*")).append(" ");
                        }
                    }
                }
                if (!addressQuery.isEmpty()) {
                    if (city != null){
                        addressQuery.append(city.getName()).append(" ");
                    }
                    if (pinCode != null){
                        addressQuery.append(pinCode.getPinCode()).append(" ");
                    }
                    addressQuery.setLength(addressQuery.length()-1);
                    addressQueries.add(addressQuery.toString());
                }
            }
            Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Address Query:" + addressQueries);
            
            return addressQueries;
        }
        

        public void setLatLng(boolean persistAfterSetting) {
            BigDecimal lat = null;
            BigDecimal lng = null;
            
            GeoCoder coder = GeoCoder.getInstance();
            if (!coder.isEnabled(params)){
                return;
            }
            for (String address : getAddressQueries(oAddress)) {
                GeoLocation location = coder.getLocation(address, params);
                if (location != null) {
                    lat = location.getLat();
                    lng = location.getLng();
                    break;
                }
            }
            
            if (lat != null && lng != null) {
                oAddress.setLat(lat);
                oAddress.setLng(lng);
            }else if (oAddress.getCityId() != null){
                oAddress.setLat(oAddress.getCity().getLat());
                oAddress.setLng(oAddress.getCity().getLng());
            }
            if (oAddress.getRawRecord().isFieldDirty("LAT") || oAddress.getRawRecord().isFieldDirty("LNG")) {
                Registry.instance().callExtensions(oAddress.getReflector().getModelClass().getSimpleName() +".after.location.set",oAddress);
                if (persistAfterSetting) {
                    oAddress.save();
                }
            }
        }

        @Override
        public void execute() {
            setLatLng(true);
        }
    }

}
