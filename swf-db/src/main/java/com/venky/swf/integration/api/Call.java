package com.venky.swf.integration.api; 

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.routing.Config;
import com.venky.xml.XMLDocument;
import org.apache.commons.io.input.ReaderInputStream;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class Call<T> {
    HttpMethod method = HttpMethod.GET;
    InputFormat inputFormat = InputFormat.FORM_FIELDS;

    String url ;
    Map<String, String> requestHeaders = new HashMap<>();

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    Map<String, List<String>> responseHeaders = new IgnoreCaseMap<>();

    ByteArrayInputStream responseStream = null;

    ByteArrayInputStream errorStream = null;

    T input;

    private void checkExpired(){
        if (responseStream != null){
            throw new RuntimeException("Call already used once. Create another instance of Call Object");
        }
    }
    public Call<T> method(HttpMethod method){
        checkExpired();
        this.method = method;
        return this;
    }
    public Call<T> inputFormat(InputFormat format){
        checkExpired();
        this.inputFormat = format;
        return this;
    }
    public Call<T> url(String url){
        checkExpired();
        this.url = url;
        return this;
    }
    public Call<T> url(String baseUrl, String relativeUrl){
        checkExpired();
        StringBuilder sUrl = new StringBuilder();
        if (baseUrl.endsWith("/")) {
            sUrl.append(baseUrl.substring(0, baseUrl.length()-1));
        }else {
            sUrl.append(baseUrl);
        }
        if (relativeUrl.startsWith("/")){
            sUrl.append(relativeUrl);
        }else {
            sUrl.append("/").append(relativeUrl);
        }
        return url(sUrl.toString());
    }

    public Call<T> headers(Map<String,String> requestHeaders){
        checkExpired();
        requestHeaders.forEach((k,v)->{
            this.requestHeaders.put(k,v);
        });
        return this;
    }
    public Call<T> header(String key, String value){
        checkExpired();
        this.requestHeaders.put(key,value);
        return this;
    }

    public Call<T> input(T input){
        checkExpired();
        this.input = input;
        return this;
    }
    public Call(){

    }

    int timeOut = 60000;
    public Call<T> timeOut(int timeOut){
        checkExpired();
        this.timeOut = timeOut;
        return this;
    }

    private Call<T> invoke(){
        checkExpired();
        if (method == HttpMethod.GET && inputFormat != InputFormat.FORM_FIELDS) {
            throw new RuntimeException("Cannot call API using Method " + method + " and parameter as " + inputFormat );
        }

        URL curl;
        HttpURLConnection connection = null;
        StringBuilder fakeCurlRequest = new StringBuilder();
        try {
            StringBuilder sUrl = new StringBuilder();
            sUrl.append(url);

            String parameterString = inputFormat == InputFormat.JSON ? getParametersAsJSONString(input) :
                    (inputFormat == InputFormat.XML ? getParametersAsXMLString(input):
                            (inputFormat == InputFormat.FORM_FIELDS ? getParametersAsFormFields(input) :
                                    ""));

            if (method == HttpMethod.GET && parameterString.length() > 0) {
                if (sUrl.lastIndexOf("?") < 0) {
                    sUrl.append("?");
                }else {
                    sUrl.append("&");
                }
                sUrl.append(parameterString);
            }
            fakeCurlRequest.append("Request ").append(":\n curl ");

            curl = new URL(sUrl.toString());
            connection = (HttpURLConnection)(curl.openConnection());

            connection.setConnectTimeout(timeOut);
            connection.setReadTimeout(timeOut);
            connection.setRequestMethod(method.toString());

            connection.setRequestProperty("Accept-Encoding", "gzip");
            for (String k : requestHeaders.keySet()) {
                String v = requestHeaders.get(k);
                connection.setRequestProperty(k, v);
                fakeCurlRequest.append(" -H '").append(k).append(": ").append(v).append("' ");
            };
            try {
                Map<String,List<String>> map = CookieManager.getDefault().get(curl.toURI(), new HashMap<>());
                for (String key: map.keySet()){
                    List<String> values = map.get(key);
                    StringBuilder value = new StringBuilder();
                    for (String v :values){
                        if (value.length() > 0){
                            value.append(";");
                        }
                        value.append(v);
                    }
                    connection.setRequestProperty(key,value.toString());
                    fakeCurlRequest.append(" -H '").append(key).append(": ").append(value.toString()).append("' ");
                };
            }catch (Exception ex){
                //
            }

            connection.setDoOutput(true);
            connection.setDoInput(true);

            fakeCurlRequest.append("'").append(sUrl).append("'");
            fakeCurlRequest.append(" ");
            if (method != HttpMethod.GET) {
                byte[] parameterByteArray = inputFormat == InputFormat.INPUT_STREAM ? getParameterRaw(input) : parameterString.getBytes();
                if (inputFormat == InputFormat.INPUT_STREAM){
                    fakeCurlRequest.append("-d '").append("**Raw binary Stream**").append("'");
                }else {
                    fakeCurlRequest.append("-d '").append(parameterString).append("'");
                }
                connection.getOutputStream().write(parameterByteArray);
            }

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 299 ) {
                //2xx is success.!!
                InputStream in = null;
                if (connection.getContentEncoding()!=null && connection.getContentEncoding().equals("gzip")) {
                    in = new GZIPInputStream(connection.getInputStream());
                }else {
                    in = connection.getInputStream();
                }
                responseHeaders.putAll(connection.getHeaderFields());

                responseStream = new ByteArrayInputStream(StringUtil.readBytes(in));
                errorStream= new ByteArrayInputStream(new byte[]{});
                this.hasErrors = false;
            }else {
                errorStream = new ByteArrayInputStream(StringUtil.readBytes(connection.getErrorStream()));
                responseStream = new ByteArrayInputStream(new byte[] {});
                this.hasErrors = true;
            }

            if (responseStream.available()> 0){
                fakeCurlRequest.append("\n Response:\n");
                fakeCurlRequest.append(StringUtil.read(responseStream,true));
            }else if (errorStream.available() >0){
                fakeCurlRequest.append("\n Error:\n");
                fakeCurlRequest.append(StringUtil.read(errorStream,true));
            }
            Config.instance().getLogger(getClass().getName()).info(fakeCurlRequest.toString());
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e); //Soften the exception.
        }finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    boolean hasErrors = false;
    public boolean hasErrors(){
        if (responseStream == null){
            invoke();
        }
        return this.hasErrors ;
    }
    @SuppressWarnings("unchecked")
    public <J extends JSONAware> J getResponseAsJson(){
        return (J)JSONValue.parse(new InputStreamReader(getResponseStream()));
    }

    public XMLDocument getResponseAsXML(){
        return XMLDocument.getDocumentFor(getResponseStream());
    }
    public InputStream getResponseStream() {
        if (responseStream == null){
            invoke();
        }
        return responseStream;
    }
    public InputStream getErrorStream() {
        if (responseStream == null){
            invoke();
        }
        return errorStream;
    }
    public String getError(){
        return Database.getJdbcTypeHelper("").getTypeRef(InputStream.class).getTypeConverter().toString(getErrorStream());
    }


    @SuppressWarnings("unchecked")
    private String getParametersAsFormFields(Object p) {
        if (p == null) {
            return "" ;
        }
        Map parameters = (Map)p;
        StringBuilder q = new StringBuilder();
        Bucket pCount = new Bucket();
        parameters.forEach((k,v)->{
            if (pCount.intValue() > 0) {
                q.append("&");
            }
            String key = (String)k;
            String value;
            try {
                if (v  instanceof List){
                    StringBuilder csv = new StringBuilder();
                    ((List) v).forEach(entry->{
                        if (csv.length() > 0){
                            csv.append(",");
                        }
                        csv.append(entry);
                    });
                    value = URLEncoder.encode(csv.toString(), "utf-8");
                }else {
                    value = URLEncoder.encode(String.valueOf(v), "utf-8");
                }
            } catch (UnsupportedEncodingException e) {
                value = String.valueOf(v);
            }
            if (q.indexOf(key+"=") < 0) {
                q.append(key).append("=").append(value);
            }
            pCount.increment();
        });
        return q.toString();
    }
    private String getParametersAsJSONString(Object p) {
        return ((JSONAware)p).toString();
    }
    private String getParametersAsXMLString(Object p) {
        return ((XMLDocument)p).toString();
    }
    private byte[] getParameterRaw(Object p){
        if (p instanceof  InputStream){
            return StringUtil.readBytes((InputStream)p);
        }else if (p instanceof Reader) {
            return StringUtil.readBytes(new ReaderInputStream((Reader)p));
        }else if (p instanceof byte[]){
            return (byte[])p;
        }else {
            throw new RuntimeException("unknown raw parameter" + p.getClass());
        }
    }
}
