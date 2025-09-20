package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;

public interface BankAccount {
    
    public String getBankName();
    public void setBankName(String bankName);

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public String getBankCode();
    public void setBankCode(String BankCode);

    String getBranchCode();
    void setBranchCode(String branchCode);
    
    
    public String getBranchName();
    public void setBranchName(String branchName);

    
   
    public String getAccountHolderName();
    public void setAccountHolderName(String accountHolderName);
    
    
    @UNIQUE_KEY("VPA")
    @IS_NULLABLE
    public String getVirtualPaymentAddress();
    public void setVirtualPaymentAddress(String VirtualPaymentAddress);
    
    
    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public String getAccountNo();
    public void setAccountNo(String AccountNo);
    
    public String getBranchAddress();
    public void setBranchAddress(String address);
    
    String getMerchantCategoryCode();
    void setMerchantCategoryCode(String merchantCategoryCode);
}
