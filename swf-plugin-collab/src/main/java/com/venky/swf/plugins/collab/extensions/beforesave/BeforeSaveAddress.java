package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.routing.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeforeSaveAddress extends BeforeModelSaveExtension<Address>{
	static {
		Registry.instance().registerExtension("Address.before.save", new BeforeSaveAddress());
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
				.append(oAddress.getPincode() == null ? "" : oAddress.getPincode())
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
				.append(oAddress.getPincode() == null ? "" : oAddress.getPincode())
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
	public void beforeSave(Address oAddress) {

		Set<String> dirtyFields = oAddress.getRawRecord().getDirtyFields();
		boolean addressFieldsChanged = dirtyFields.contains("ADDRESS_LINE_1") || dirtyFields.contains("ADDRESS_LINE_2") || dirtyFields.contains("ADDRESS_LINE_3") ||
				dirtyFields.contains("ADDRESS_LINE_4") || dirtyFields.contains("CITY_ID") || dirtyFields.contains("STATE_ID") || dirtyFields.contains("COUNTRY_ID") || dirtyFields.contains("PIN_CODE");
		if (!addressFieldsChanged) {
			return;
		}
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
		
	}

}
