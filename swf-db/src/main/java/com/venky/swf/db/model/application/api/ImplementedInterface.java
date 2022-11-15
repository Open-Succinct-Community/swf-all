package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;

public interface ImplementedInterface extends Model {
    @HIDDEN
    public Long getEndPointId();
    public void setEndPointId(Long id);
    public EndPoint getEndPoint();

    public Long getInterfaceId();
    public void setInterfaceId(Long id);
    public Interface getInterface();




}
