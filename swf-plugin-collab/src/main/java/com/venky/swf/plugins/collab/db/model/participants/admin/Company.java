package com.venky.swf.plugins.collab.db.model.participants.admin;


import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.io.InputStream;
import java.sql.Date;
import java.util.List;

public interface Company extends Model{
	@IS_VIRTUAL
	@PARTICIPANT
	@HIDDEN
	public Long getAnyUserId();
	public void setAnyUserId(Long anyUserId);
	public User getAnyUser();

	@COLUMN_NAME("ID")
	@PROTECTION
	@HIDDEN
	@HOUSEKEEPING
	@PARTICIPANT
	public long getSelfCompanyId();
	public void setSelfCompanyId(long id);
	
	@IS_VIRTUAL
	public Company getSelfCompany();


	@COLUMN_NAME("ID")
	@PROTECTION
	@HIDDEN
	@HOUSEKEEPING
	@PARTICIPANT
	public long getVendorId();
	public void setVendorId(long id);

	@IS_VIRTUAL
	public Company getVendor();
	
	
	@COLUMN_NAME("ID")
	@PROTECTION
	@HIDDEN
	@HOUSEKEEPING
	@PARTICIPANT
	public long getCustomerId();
	public void setCustomerId(long id);

	@IS_VIRTUAL
	public Company getCustomer();


	@IS_NULLABLE(false)
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);

	@Index
	public String getDomainName();
	public void setDomainName(String domainName);

	public Date getDateOfIncorporation();
	public void setDateOfIncorporation(Date date);
	
	@CONTENT_TYPE(MimeType.IMAGE_PNG)
	public InputStream getLogo();
	public void setLogo(InputStream in);
	
	@PROTECTION(Kind.NON_EDITABLE)
	@HIDDEN
	public String getLogoContentName();
	public void setLogoContentName(String name);

	@PROTECTION(Kind.NON_EDITABLE)
	@HIDDEN
	public String getLogoContentType();
	public void setLogoContentType(String contentType);
	
	@PROTECTION(Kind.NON_EDITABLE)
	@HIDDEN
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getLogoContentSize();
	public void setLogoContentSize(int size);

	
	public List<Facility> getFacilities();
	
	@CONNECTED_VIA("COMPANY_ID")
	public List<User> getUsers();

	public List<Long> getStaffUserIds();


	@CONNECTED_VIA("CUSTOMER_ID")
	public List<CompanyRelationship> getVendors();

	@CONNECTED_VIA("VENDOR_ID")
	public List<CompanyRelationship> getCustomers();

	@PARTICIPANT
	public Long getCreatorCompanyId();
	public void setCreatorCompanyId( Long id);
	public Company getCreatorCompany();

	@HIDDEN
	@CONNECTED_VIA("CREATOR_COMPANY_ID")
	public List<Company> getCreatedCompanies();

}
