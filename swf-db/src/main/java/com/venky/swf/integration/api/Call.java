package com.venky.swf.integration.api;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.routing.Config;
import com.venky.xml.XMLDocument;
import org.apache.commons.io.input.ReaderInputStream;
import org.json.simple.JSONAware;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow.Subscriber;
import java.util.zip.GZIPInputStream;

public class Call<T> implements Serializable {
    HttpMethod method = null;
    InputFormat inputFormat = InputFormat.FORM_FIELDS;

    String url ;
    Map<String, String> requestHeaders = new IgnoreCaseMap<>();

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    private transient Map<String, List<String>> responseHeaders = new IgnoreCaseMap<>();
    private transient  ByteArrayInputStream responseStream = null;
    private transient ByteArrayInputStream errorStream = null;
    private transient int status = -1;
    public int getStatus(){
        return status;
    }
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
        if (url != null) {
            try {
                this.url = new URI(url).normalize().toString();
            }catch (Exception ex){
                this.url = url;
            }
        }

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

    long timeOut = 60000L;
    public Call<T> timeOut(long timeOut){
        checkExpired();
        this.timeOut = timeOut;
        return this;
    }


    private Call<T> invoke() {
        checkExpired();
        
        if (method == null ) {
            if (inputFormat != InputFormat.FORM_FIELDS) {
                method = HttpMethod.POST;
            }else {
                method = HttpMethod.GET;
            }
        }
        Builder curlBuilder ;
        StringBuilder fakeCurlRequest = new StringBuilder();
        try {
            StringBuilder sUrl = new StringBuilder();
            sUrl.append(url);

            String parameterString = inputFormat == InputFormat.JSON ? getParametersAsJSONString(input) :
                    (inputFormat == InputFormat.XML ? getParametersAsXMLString(input):
                            (inputFormat == InputFormat.FORM_FIELDS ? getParametersAsFormFields(input) :
                                    ""));

            if (method == HttpMethod.GET && !parameterString.isEmpty() && inputFormat == InputFormat.FORM_FIELDS) {
                if (sUrl.lastIndexOf("?") < 0) {
                    sUrl.append("?");
                }else {
                    sUrl.append("&");
                }
                sUrl.append(parameterString);
            }
            fakeCurlRequest.append("Request ").append(":\n curl ");

            curlBuilder = HttpRequest.newBuilder().uri(new URI(sUrl.toString()));
            curlBuilder.timeout(Duration.ofMillis(timeOut));
            byte[] parameterByteArray = inputFormat == InputFormat.INPUT_STREAM ? getParameterRaw(input) : parameterString.getBytes();
            if (method ==  HttpMethod.GET){
                if (inputFormat == InputFormat.FORM_FIELDS) {
                    curlBuilder.GET();
                }else {
                    curlBuilder.method("GET",BodyPublishers.ofByteArray(parameterByteArray));
                }
            }else {
                curlBuilder.POST(BodyPublishers.ofByteArray(parameterByteArray));
            }
            curlBuilder.setHeader("Accept-Encoding", "gzip");
            curlBuilder.version(Version.HTTP_2);

            for (String k : requestHeaders.keySet()) {
                String v = requestHeaders.get(k);
                curlBuilder.setHeader(k, v);
                if (Config.instance().isDevelopmentEnvironment()) {
                    fakeCurlRequest.append(" -H '").append(k).append(": ").append(v).append("' ");
                }else {
                    fakeCurlRequest.append(" -H '").append(k).append(": ").append("****").append("' ");
                }
            }


            fakeCurlRequest.append("'").append(sUrl).append("'");
            fakeCurlRequest.append(" ");
            if (method != HttpMethod.GET) {
                if (inputFormat == InputFormat.INPUT_STREAM){
                    String contentType = requestHeaders.get("content-type");
                    MimeType mimeType = null;
                    if (!ObjectUtil.isVoid(contentType)){
                        mimeType = MimeType.getMimeType(contentType);
                    }
                    if (ObjectUtil.isVoid(contentType) || mimeType == null || !mimeType.isText() ){
                        fakeCurlRequest.append("-d '").append("**Raw binary Stream**").append("'");
                    }else {
                        fakeCurlRequest.append("-d '").append(new String(parameterByteArray)).append("'");
                    }
                }else {
                    fakeCurlRequest.append("-d '").append(parameterString).append("'");
                }
            }
            HttpRequest request  = curlBuilder.build();
            HttpResponse<InputStream> response = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());

            this.status = response.statusCode();

            if (response.statusCode() >= 200 && response.statusCode() < 299 ) {
                //2xx is success.!!
                InputStream in = isResponseDecompressed() && response.headers().firstValue("Content-Encoding").isPresent() ? new GZIPInputStream(response.body()) : response.body();
                responseHeaders.putAll(response.headers().map());
                responseStream = new ByteArrayInputStream(StringUtil.readBytes(in));
                errorStream= new ByteArrayInputStream(new byte[]{});
                this.hasErrors = false;
            }else {
                InputStream in = isResponseDecompressed() && response.headers().firstValue("Content-Encoding").isPresent() ? new GZIPInputStream(response.body()) : response.body();
                errorStream = new ByteArrayInputStream(StringUtil.readBytes(in));
                responseStream = new ByteArrayInputStream(new byte[] {});
                this.hasErrors = true;
            }

            if (responseStream.available()> 0){
                fakeCurlRequest.append("\n Response:\n");
                String contentType = responseHeaders.get("content-type").isEmpty() ? MimeType.TEXT_PLAIN.toString() : responseHeaders.get("content-type").get(0);
                if (contentType.startsWith(MimeType.APPLICATION_JSON.toString()) ||
                        contentType.startsWith(MimeType.APPLICATION_XML.toString()) ||
                        contentType.startsWith("text")){
                    fakeCurlRequest.append(StringUtil.read(responseStream,true));
                }else {
                    fakeCurlRequest.append("**Raw binary Stream**");
                }
            }else if (errorStream.available() >0){
                fakeCurlRequest.append("\n Status: ").append(response.statusCode());
                fakeCurlRequest.append("\n Error:\n");
                fakeCurlRequest.append(StringUtil.read(errorStream,true));
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e); //Soften the exception.
        }finally {
            Config.instance().getLogger(getClass().getName()).info(fakeCurlRequest.toString());
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

    boolean responseDecompressed = true;
    public boolean isResponseDecompressed() {
        return responseDecompressed;
    }
    public void setResponseDecompressed(boolean responseDecompressed) {
        this.responseDecompressed = responseDecompressed;
    }
}
