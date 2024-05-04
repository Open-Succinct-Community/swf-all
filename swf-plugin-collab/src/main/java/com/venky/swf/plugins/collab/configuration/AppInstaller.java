package com.venky.swf.plugins.collab.configuration;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.WorldCurrency;
import com.venky.swf.plugins.collab.db.model.config.WorldLanguage;
import com.venky.swf.plugins.collab.db.model.config.WorldTimeZone;

import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TimeZone;

public class AppInstaller implements Installer {
    public void install(){
        installCountries();
        installLanguages();
        installTimeZones();
        installCurrencies();
    }
    public void installCurrencies(){
        long count = Database.getTable(WorldCurrency.class).recordCount();
        if (count > 0){
            return;
        }
        Map<String, WorldCurrency> currencyMap = new Cache<>(0,0){

            @Override
            protected WorldCurrency getValue(String code) {
                WorldCurrency l =  Database.getTable(WorldCurrency.class).newRecord();
                l.setCode(code);
                return l;
            }
        };
        Currency.getAvailableCurrencies().forEach(c->{
            WorldCurrency worldCurrency = currencyMap.get(c.getCurrencyCode());
            worldCurrency.setName(c.getDisplayName());
            worldCurrency.setSymbol(c.getSymbol());
        });
        currencyMap.values().forEach(c->c.save());

    }
    public void installCountries(){
        long count = Database.getTable(Country.class).recordCount();
        if (count > 0){
            return;
        }
        Map<String, Country> countryMap = new Cache<>(0,0){

            @Override
            protected Country getValue(String iso2) {
                Country l =  Database.getTable(Country.class).newRecord();
                l.setIsoCode2(iso2);
                return l;
            }
        };
        Arrays.stream(Locale.getAvailableLocales()).forEach(l->{
            if (ObjectUtil.isVoid(l.getCountry())){
                return;
            }
            Country country= countryMap.get(l.getCountry());
            country.setName(l.getDisplayCountry());
            try {
                country.setIsoCode(l.getISO3Country());
            }catch (MissingResourceException ex){
                country.setIsoCode(country.getIsoCode2());
            }
        });
        countryMap.values().forEach(o->o.save());
    }
    public void installTimeZones(){
        long count = Database.getTable(WorldTimeZone.class).recordCount();
        if (count > 0){
            return;
        }
        Map<String, WorldTimeZone> tzMap = new Cache<>(0,0){

            @Override
            protected WorldTimeZone getValue(String zId) {
                WorldTimeZone l =  Database.getTable(WorldTimeZone.class).newRecord();
                l.setZoneId(zId);
                return l;
            }
        };
        Arrays.stream(TimeZone.getAvailableIDs()).forEach(id->{
            TimeZone timeZone = TimeZone.getTimeZone(id);
            WorldTimeZone wtz= tzMap.get(timeZone.getID());
            wtz.setName(timeZone.getDisplayName());
        });
        tzMap.values().forEach(tz->tz.save());

    }
    public void installLanguages(){
        long count = Database.getTable(WorldLanguage.class).recordCount();
        if (count > 0){
            return;
        }
        Map<String, WorldLanguage> languageMap = new Cache<>(0,0){

            @Override
            protected WorldLanguage getValue(String iso2) {
                WorldLanguage l =  Database.getTable(WorldLanguage.class).newRecord();
                l.setIsoCode2(iso2);
                return l;
            }
        };
        Arrays.stream(Locale.getAvailableLocales()).forEach(l->{
            WorldLanguage language = languageMap.get(l.getLanguage());
            language.setIsoCode(l.getISO3Language());
            language.setName(l.getDisplayLanguage());
        });
        for (WorldLanguage l : languageMap.values()) {
            l.save();
        }
    }
}
