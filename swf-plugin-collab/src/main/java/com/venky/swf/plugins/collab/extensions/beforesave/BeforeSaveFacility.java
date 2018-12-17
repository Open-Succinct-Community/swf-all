package com.venky.swf.plugins.collab.extensions.beforesave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.string.StringUtil; 
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.routing.Config;

public class BeforeSaveFacility extends BeforeModelSaveExtension<Facility>{
	static {
		Registry.instance().registerExtension("Facility.before.save", new BeforeSaveFacility());
	}
	
	private StringBuilder[] getAddressQueries(Facility facility){
		StringBuilder[] address = new StringBuilder[]{
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine1()))
				.append(" ")
				.append(StringUtil.valueOf(facility.getAddressLine2()))
				.append(" ")
				.append(StringUtil.valueOf(facility.getAddressLine3()))
				.append(" ")
				.append(StringUtil.valueOf(facility.getAddressLine4()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getState() == null ? "" : facility.getState().getName())
				.append(" ")
				.append(facility.getPincode() == null ? "" : facility.getPincode())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine2()))
				.append(" ")
				.append(StringUtil.valueOf(facility.getAddressLine3()))
				.append(" ")
				.append(StringUtil.valueOf(facility.getAddressLine4()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getState() == null ? "" : facility.getState().getName())
				.append(" ")
				.append(facility.getPincode() == null ? "" : facility.getPincode())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine4()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getState() == null ? "" : facility.getState().getName())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine4()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
		};
		return address;
	}
	@Override
	public void beforeSave(Facility facility) {
		List<GeoCoder> coders = new ArrayList<GeoCoder>();
		coders.add(new GeoCoder("here"));
		coders.add(new GeoCoder("google"));
		coders.add(new GeoCoder("openstreetmap"));
		Map<String,String> params = new HashMap<>();
		params.put("here.app_id",Config.instance().getProperty("geocoder.here.app_id"));
		params.put("here.app_code",Config.instance().getProperty("geocoder.here.app_code"));
		params.put("google.api_key",Config.instance().getProperty("geocoder.google.api_key"));

		for (Iterator<GeoCoder> i = coders.iterator(); i.hasNext() && (ObjectUtil.isVoid(facility.getLat()) || ObjectUtil.isVoid(facility.getLng())) ; ){
			GeoCoder coder = i.next(); 
			for (StringBuilder address: getAddressQueries(facility)){
				GeoLocation location = coder.getLocation(address.toString(),params);
				if (location != null){ 
					facility.setLat(location.getLat());
					facility.setLng(location.getLng());
					break ;
				}
			} 
		}
		
	}

}
