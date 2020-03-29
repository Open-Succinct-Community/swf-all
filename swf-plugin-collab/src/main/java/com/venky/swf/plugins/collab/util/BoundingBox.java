package com.venky.swf.plugins.collab.util;

import com.venky.geo.GeoCoder;
import com.venky.geo.GeoCoordinate;
import com.venky.geo.GeoDistance;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.math.BigDecimal;
import java.util.List;

public class BoundingBox {
    private GeoCoordinate min;
    private GeoCoordinate max;
    private GeoCoordinate closeTo;
    public BoundingBox(GeoCoordinate closeTo, double accuracyKM, double withinKms){
        this.closeTo = closeTo;
        double lat = closeTo.getLat().doubleValue();
        double lng = closeTo.getLng().doubleValue();

        double F = (withinKms + accuracyKM)/ GeoCoordinate.R;
        double deltaLat = F * 180 / Math.PI;
        double maxLat = lat + deltaLat;
        double minLat = lat - deltaLat;

        double cosdeltaLng = (Math.cos(F) - Math.pow(Math.sin(lat * Math.PI/180.0),2)) / Math.pow(Math.cos(lat * Math.PI / 180.0), 2);
        cosdeltaLng = Math.max(Math.min(1,cosdeltaLng),-1);

        double deltaLng = Math.acos(cosdeltaLng) * 180 / Math.PI ;


        double maxLng = lng + deltaLng ;
        double minLng = lng - deltaLng ;

        min = new GeoCoordinate(new BigDecimal(minLat),new BigDecimal(minLng));
        max = new GeoCoordinate(new BigDecimal(maxLat),new BigDecimal(maxLng));
    }

    public GeoCoordinate getMin(){
        return min;
    }
    public GeoCoordinate getMax() {
        return max;
    }

    /**
     * Find Records of modelClass that lie within the bounding box.
     * @param modelClass
     * @param <T>
     * @return
     */
    public <T extends GeoLocation & Model> List<T> find(Class<T> modelClass, int limit){
        ModelReflector<T> ref = ModelReflector.instance(modelClass) ;


        Expression where = new Expression(ref.getPool(),Conjunction.AND);
        String LAT = ref.getColumnDescriptor("LAT").getName();
        String LNG = ref.getColumnDescriptor("LNG").getName();


        where.add(new Expression(ref.getPool(),LAT, Operator.GE, min.getLat()));
        where.add(new Expression(ref.getPool(),LAT, Operator.LT, max.getLat()));
        where.add(new Expression(ref.getPool(),LNG, Operator.GE, min.getLng()));
        where.add(new Expression(ref.getPool(),LNG, Operator.LT, max.getLng()));

        Select select = new Select().from(modelClass).where(where);
        select.add(" ORDER BY ABS(LAT - "+this.closeTo.getLat() + ") , ABS(LNG - "+this.closeTo.getLng() + ")");

        List<T> objects = select.execute(modelClass,limit);
        return objects;
    }
}
