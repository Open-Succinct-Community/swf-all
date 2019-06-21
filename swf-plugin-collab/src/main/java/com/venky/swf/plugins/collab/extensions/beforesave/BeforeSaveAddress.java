package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.routing.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeforeSaveAddress<M extends Address & Model> extends BeforeModelSaveExtension<M>{


	protected String[] getAddressFields(){
		return new String[]{"ADDRESS_LINE_1","ADDRESS_LINE_2","ADDRESS_LINE_3","ADDRESS_LINE_4","CITY_ID","STATE_ID","COUNTRY_ID","PIN_CODE"};
	}
	protected boolean isAddressChanged(M oAddress){
		Set<String> dirtyFields = oAddress.getRawRecord().getDirtyFields();
		boolean addressFieldsChanged = false;
		for (String field : getAddressFields()){
			addressFieldsChanged = addressFieldsChanged || dirtyFields.contains(field);
			if (addressFieldsChanged){
				break;
			}
		}
		return addressFieldsChanged;
	}
	@Override
	public void beforeSave(M oAddress) {
		if (!isAddressChanged(oAddress)) {
			return;
		}


		if (oAddress.getCityId() != null){
			oAddress.setStateId(oAddress.getCity().getStateId());
		}
		if (oAddress.getStateId() != null){
			oAddress.setCountryId(oAddress.getState().getCountryId());
		}
		if (!oAddress.getReflector().isVoid(oAddress.getLat()) && !oAddress.getReflector().isVoid(oAddress.getLng())){
			if (oAddress.getRawRecord().isNewRecord()){
				//Prefilled at creation. So ignore regetting it.
				return;
			}
		}
        TaskManager.instance().executeAsync(new LocationSetterTask<M>(oAddress),false);
	}

	public static class LocationSetterTask<M extends Address & Model> implements Task {
	    M oAddress = null;
        public LocationSetterTask(M address){
            this.oAddress = address;
        }
        private StringBuilder[] getAddressQueries(Address oAddress){
            StringBuilder[] address = new StringBuilder[]{
                    new StringBuilder()
                            .append(StringUtil.valueOf(oAddress.getAddressLine1()))
                            .append(" ")
                            .append(StringUtil.valueOf(oAddress.getAddressLine2()))
                            .append(" ")
                            .append(StringUtil.valueOf(oAddress.getAddressLine3()))
                            .append(" ")
                            .append(StringUtil.valueOf(oAddress.getAddressLine4()))
                            .append(" ")
                            .append(oAddress.getCity() == null ? "" : oAddress.getCity().getName())
                            .append(" ")
                            .append(oAddress.getState() == null ? "" : oAddress.getState().getName())
                            .append(" ")
                            .append(oAddress.getPinCode() == null ? "" : oAddress.getPinCode().getPinCode())
                            .append(" ")
                            .append(oAddress.getCountry() == null ? "" : oAddress.getCountry().getName()),
                    new StringBuilder()
                            .append(StringUtil.valueOf(oAddress.getAddressLine2()))
                            .append(" ")
                            .append(StringUtil.valueOf(oAddress.getAddressLine3()))
                            .append(" ")
                            .append(StringUtil.valueOf(oAddress.getAddressLine4()))
                            .append(" ")
                            .append(oAddress.getCity() == null ? "" : oAddress.getCity().getName())
                            .append(" ")
                            .append(oAddress.getState() == null ? "" : oAddress.getState().getName())
                            .append(" ")
                            .append(oAddress.getPinCode() == null ? "" : oAddress.getPinCode().getPinCode())
                            .append(" ")
                            .append(oAddress.getCountry() == null ? "" : oAddress.getCountry().getName()),
                    new StringBuilder()
                            .append(StringUtil.valueOf(oAddress.getAddressLine4()))
                            .append(" ")
                            .append(oAddress.getCity() == null ? "" : oAddress.getCity().getName())
                            .append(" ")
                            .append(oAddress.getState() == null ? "" : oAddress.getState().getName())
                            .append(" ")
                            .append(oAddress.getCountry() == null ? "" : oAddress.getCountry().getName()),
                    new StringBuilder()
                            .append(StringUtil.valueOf(oAddress.getAddressLine4()))
                            .append(" ")
                            .append(oAddress.getCity() == null ? "" : oAddress.getCity().getName())
                            .append(" ")
                            .append(oAddress.getCountry() == null ? "" : oAddress.getCountry().getName()),
            };
            return address;
        }

        @Override
        public void execute() {
            oAddress.setLat(null);
            oAddress.setLng(null);

            List<GeoCoder> coders = new ArrayList<GeoCoder>();
            coders.add(new GeoCoder("google"));
            coders.add(new GeoCoder("openstreetmap"));
            coders.add(new GeoCoder("here"));
            Map<String,String> params = new HashMap<>();
            params.put("here.app_id",Config.instance().getProperty("geocoder.here.app_id"));
            params.put("here.app_code",Config.instance().getProperty("geocoder.here.app_code"));
            params.put("google.api_key",Config.instance().getProperty("geocoder.google.api_key"));

            for (Iterator<GeoCoder> i = coders.iterator(); i.hasNext() && (ObjectUtil.isVoid(oAddress.getLat()) || ObjectUtil.isVoid(oAddress.getLng())) ; ){
                GeoCoder coder = i.next();
                for (StringBuilder address: getAddressQueries(oAddress)){
                    GeoLocation location = coder.getLocation(address.toString(),params);
                    if (location != null){
                        oAddress.setLat(location.getLat());
                        oAddress.setLng(location.getLng());
                        break ;
                    }
                }
            }
            oAddress.save();
        }
    }

}
