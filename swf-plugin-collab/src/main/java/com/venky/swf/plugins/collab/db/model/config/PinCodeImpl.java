package com.venky.swf.plugins.collab.db.model.config;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PinCodeImpl extends ModelImpl<PinCode> {
    public PinCodeImpl(){
        super();
    }
    public PinCodeImpl(PinCode pinCode){
        super(pinCode);
    }

    private void loadPostalOffice(){
        if (fieldValues.isEmpty()){
            PinCode pinCode = getProxy();
            if (!ObjectUtil.isVoid(pinCode.getPinCode())){
                List<PostalOffice> postalOfficeList = new Select().from(PostalOffice.class).where(new Expression(ModelReflector.instance(PostalOffice.class).getPool(),"PIN_CODE", Operator.EQ, pinCode.getPinCode())).execute();
                
                for (PostalOffice postalOffice : postalOfficeList) {
                    if (postalOffice.getCountryId() != null){
                        fieldValues.get("COUNTRY_ID").add(postalOffice.getCountryId());
                    }
                    if (postalOffice.getStateId() != null){
                        fieldValues.get("STATE_ID").add(postalOffice.getStateId());
                    }else if (!ObjectUtil.isVoid(postalOffice.getStateName())){
                        List<State> states = new Select().from(State.class).where(
                                new Expression(ModelReflector.instance(State.class).getPool(), Conjunction.AND){{
                                    if (!fieldValues.get("COUNTRY_ID").isEmpty()) {
                                        add(new Expression(ModelReflector.instance(State.class).getPool(), "COUNTRY_ID", Operator.IN, fieldValues.get("COUNTRY_ID").toArray()));
                                    }
                                    add(new Expression(ModelReflector.instance(State.class).getPool(),"lower(NAME)", Operator.EQ, postalOffice.getStateName().toLowerCase()));
                                }}).execute();
                        for (State state : states) {
                            fieldValues.get("STATE_ID").add(state.getId());
                        }
                    }
                    if (postalOffice.getCityId() != null) {
                        fieldValues.get("CITY_ID").add(postalOffice.getCityId());
                    }else {
                        for (String name : new String[]{postalOffice.getTaluk(), postalOffice.getDistrict() }){
                            if (!ObjectUtil.isVoid(name)){
                                Select select = new Select().from(City.class);
                                
                                List<City> cities = select.where(
                                                new Expression(select.getPool(),Conjunction.AND){{
                                                    if (!fieldValues.get("STATE_ID").isEmpty()) {
                                                        add(new Expression(select.getPool(), "STATE_ID", Operator.IN, fieldValues.get("STATE_ID").toArray()));
                                                    }
                                                    add(new Expression(select.getPool(),"lower(NAME)", Operator.EQ, name.toLowerCase()));
                                                }})
                                        .execute();
                                for (City city : cities) {
                                    fieldValues.get("CITY_ID").add(city.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private Map<String, Set<Long>> fieldValues = new Cache<String, Set<Long>>() {
        @Override
        protected Set<Long> getValue(String s) {
            return new HashSet<>();
        }
    } ;
    public Long getStateId() {
        loadPostalOffice();
        Set<Long> stateIds = fieldValues.get("STATE_ID");
        return stateIds.size() == 1 ? stateIds.iterator().next() : null;
    }
    public void setStateId(Long id){

    }

    public Long getCityId() {
        loadPostalOffice();
        Set<Long> cityIds = fieldValues.get("CITY_ID");
        return cityIds.size() == 1 ? cityIds.iterator().next() : null ;
    }
    public void setCityId(Long id){

    }
    
}
