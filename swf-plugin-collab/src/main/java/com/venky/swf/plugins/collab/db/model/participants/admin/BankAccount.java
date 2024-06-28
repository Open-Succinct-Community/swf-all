package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface BankAccount {
    public String getBankName();
    public void setBankName(String bankName);

    public String getBankCode();
    public void setBankCode(String BankCode);

    public String getBranchName();
    public void setBranchName(String branchName);

    public String getAccountNo();
    public void setAccountNo(String AccountNo);


    public String getVirtualPaymentAddress();
    public void setVirtualPaymentAddress(String VirtualPaymentAddress);
}
