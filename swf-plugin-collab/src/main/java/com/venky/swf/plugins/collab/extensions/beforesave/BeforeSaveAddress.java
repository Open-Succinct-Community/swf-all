package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.collections.SequenceMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
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
            LocationSetterTask<M> setterTask = new LocationSetterTask<M>(oAddress);
            if (isOkToSetLocationAsync()) {
                TaskManager.instance().executeAsync(setterTask, false);
            } else {
                setterTask.setLatLng(false);
            }
        }
    }

    public static class LocationSetterTask<M extends Address & Model> implements Task {

        M oAddress = null;
        Map<String, String> params = new HashMap<>();
        public LocationSetterTask(M address){
            this.oAddress = address;
            this.params = Config.instance().getGeoProviderParams();
        }

        private Set<String> getAddressQueries(M oAddress) {
            SequenceMap<String, String> priorityFields = new SequenceMap<>();
            for (String f : Address.getAddressFields()) {
                if (!oAddress.getReflector().isVoid(oAddress.getReflector().get(oAddress, f))) {
                    priorityFields.put(f, StringUtil.valueOf(oAddress.getReflector().get(oAddress, f)));
                }
            }

            if (priorityFields.containsKey("CITY_ID")) {
                City city = oAddress.getCity();
                State state = oAddress.getStateId() != null ? oAddress.getState() : ( city != null ? city.getState() : null ) ;
                Country country = oAddress.getCountryId() != null ? oAddress.getCountry() : ( state != null ? state.getCountry() : null );

                if (city != null) {
                    priorityFields.put("CITY_ID",city.getName());
                }
                if (state != null) {
                    priorityFields.put("STATE_ID",state.getName());
                }
                if (country != null) {
                    priorityFields.put("COUNTRY_ID",country.getName());
                }
            }
            if (priorityFields.containsKey("PIN_CODE_ID")) {
                priorityFields.put("PIN_CODE_ID",oAddress.getPinCode().getPinCode());
            }

            SequenceSet<String> addressQueries = new SequenceSet<>();
            /*
            for (int i = priorityFields.size() - 1; i >= 0; i--) {
                StringBuilder addressQuery = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    addressQuery.insert(0, " ");
                    addressQuery.insert(0, priorityFields.getValueAt(priorityFields.size() - 1 - j));
                }
                addressQueries.add(addressQuery.toString());
            }
            
             */
            StringBuilder addressQuery = new StringBuilder();
            for (String value : priorityFields.values()) {
                if (!addressQuery.isEmpty()){
                    addressQuery.append(" ");
                }
                addressQuery.append(value);
            }
            addressQueries.add(addressQuery.toString());
            
            //Just one query is enough! plugin osm can be used effectively.

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
