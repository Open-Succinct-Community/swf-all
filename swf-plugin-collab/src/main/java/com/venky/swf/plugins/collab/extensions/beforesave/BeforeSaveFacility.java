package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.core.string.StringUtil;
import com.venky.extension.Registry;
import com.venky.geo.GeoCoder;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;

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
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getState() == null ? "" : facility.getState().getName())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine1()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getState() == null ? "" : facility.getState().getName())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
				new StringBuilder()
				.append(StringUtil.valueOf(facility.getAddressLine1()))
				.append(" ")
				.append(facility.getCity() == null ? "" : facility.getCity().getName())
				.append(" ")
				.append(facility.getCountry() == null ? "" : facility.getCountry().getName()),
		};
		return address;
	}
	@Override
	public void beforeSave(Facility facility) {
		for (StringBuilder address: getAddressQueries(facility)){
			GeoLocation location = GeoCoder.getLocation(address.toString());
			if (location != null){ 
				facility.setLatitude(location.getLatitude());
				facility.setLongitude(location.getLongitude());
				break ;
			}
		}
	}

}
