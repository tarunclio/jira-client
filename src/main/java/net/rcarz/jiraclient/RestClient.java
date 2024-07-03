/**
 * jira-client - a simple JIRA REST client
 * Copyright (c) 2013 Bob Carroll (bob.carroll@alum.rit.edu)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.rcarz.jiraclient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A simple REST client that speaks JSON.
 */

@FunctionalInterface
interface JsonProcessor {
    boolean  isJsonArray(String jsonString) throws JSONException;
}


public class RestClient {

    private HttpClient httpClient = null;
    private ICredentials creds = null;
    private URI uri = null;
 
    /**
     * Creates a REST client instance with a URI.
     *
     * @param httpclient Underlying HTTP client to use
     * @param uri Base URI of the remote REST service
     */
    public RestClient(HttpClient httpclient, URI uri) {
        this(httpclient, null, uri);
    }

    /**
     * Creates an authenticated REST client instance with a URI.
     *
     * @param httpclient Underlying HTTP client to use
     * @param creds Credentials to send with each request
     * @param uri Base URI of the remote REST service
     */
    public RestClient(HttpClient httpclient, ICredentials creds, URI uri) {
        this.httpClient = httpclient;
        this.creds = creds;
        this.uri = uri;
    }

    /**
     * Build a URI from a path.
     *
     * @param path Path to append to the base URI
     *
     * @return the full URI
     *
     * @throws URISyntaxException when the path is invalid
     */
    public URI buildURI(String path) throws URISyntaxException {
        return buildURI(path, null);
    }

    /**
     * Build a URI from a path and query parmeters.
     *
     * @param path Path to append to the base URI
     * @param params Map of key value pairs
     *
     * @return the full URI
     *
     * @throws URISyntaxException when the path is invalid
     */
    public URI buildURI(String path, Map<String, String> params) throws URISyntaxException {
        URIBuilder ub = new URIBuilder(uri.toString());
        
        System.out.println("BUILDURI "+ub.toString()+" path "+path+ " map is "+params);
       
        ub.setPath(path);
        System.out.println("NOW URI "+ub.toString()+" path "+path+ " map is "+params);

        if (params != null) {
            for (Map.Entry<String, String> ent : params.entrySet())
                ub.addParameter(ent.getKey(), ent.getValue());
        }
        System.out.println("RETURNING "+ub.build().toString());

        return ub.build();
    }

    private JSONObject request(HttpRequestBase req) throws RestException, IOException {

        HttpResponse  resp = null;
        JSONObject result =null;
        System.out.println("SENDING REQ to uri "+req.getURI().toString());

        StatusLine sl = null;
        req.setHeader("Accept", "application/json");
        try {
        if (creds != null)
            creds.authenticate(req);

          resp = httpClient.execute(req);
        HttpEntity ent = resp.getEntity();
        
        if (ent != null) {
        	String str = EntityUtils.toString(ent,StandardCharsets.UTF_8);
        	System.out.println("JSON STR "+str);
        	


        sl = resp.getStatusLine();
        
    	Object  jsonValue = new JSONTokener(str).nextValue();
        if (jsonValue instanceof JSONObject) {
            // Handle it as a JSONObject
           result = (JSONObject) jsonValue;
           
           
        } else if (jsonValue instanceof JSONArray) {
            // Handle it as a JSONArray
            JSONArray jsonArray = (JSONArray) jsonValue;
            result = new JSONObject("{}");
            result.put("array", jsonArray);
        }
      //  System.out.println("RESULT "+result.toString(2));
        } 
        } catch (Exception e ) {
        	System.out.println("WE HAVE TROUBLE "+e.getMessage());
        	e.printStackTrace();
        }
        
        if (sl.getStatusCode() >= 300)
            throw new RestException(sl.getReasonPhrase(), sl.getStatusCode(), result.toString());
      
        //return result.length() > 0 ? new JSONObject(result.toString()): null;
        return result;
    }

    private JSONObject request(HttpEntityEnclosingRequestBase req, String payload)
        throws RestException, IOException {

        if (payload != null) {
            StringEntity ent = null;

            try {
                ent = new StringEntity(payload, "UTF-8");
                ent.setContentType("application/json");
            } 
            catch (Exception ex) {
               /* utf-8 should always be supported... */
            }

            req.addHeader("Content-Type", "application/json");
            req.setEntity(ent);
        }

        return request(req);
    }
    
    private JSONObject request(HttpEntityEnclosingRequestBase req, File file)
            throws RestException, IOException {
            if (file != null) {
            	File fileUpload = file;
            	req.setHeader("X-Atlassian-Token","nocheck");
            	MultipartEntity ent = new MultipartEntity();
            	ent.addPart("file", new FileBody(fileUpload));
            	req.setEntity(ent);
            }
            return request(req);
        }

    private JSONObject request(HttpEntityEnclosingRequestBase req, JSONObject payload)
        throws RestException, IOException {

        return request(req, payload != null ? payload.toString() : null);
    }

    /**
     * Executes an HTTP DELETE with the given URI.
     *
     * @param uri Full URI of the remote endpoint
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     */
    public JSONObject delete(URI uri) throws RestException, IOException {
        return request(new HttpDelete(uri));
    }

    /**
     * Executes an HTTP DELETE with the given path.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */
    public JSONObject delete(String path) throws RestException, IOException, URISyntaxException {
        return delete(buildURI(path));
    }

    /**
     * Executes an HTTP GET with the given URI.
     *
     * @param uri Full URI of the remote endpoint
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     */
    public JSONObject get(URI uri) throws RestException, IOException {
        return request(new HttpGet(uri));
    }

    /**
     * Executes an HTTP GET with the given path.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     * @param params Map of key value pairs
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */
    public JSONObject get(String path, Map<String, String> params) throws RestException, IOException, URISyntaxException {
        return get(buildURI(path, params));
    }

    /**
     * Executes an HTTP GET with the given path.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */
    public JSONObject get(String path) throws RestException, IOException, URISyntaxException {
        return get(path, null);
    }


    /**
     * Executes an HTTP POST with the given URI and payload.
     *
     * @param uri Full URI of the remote endpoint
     * @param payload JSON-encoded data to send to the remote service
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     */
    public JSONObject post(URI uri, JSONObject payload) throws RestException, IOException {
        return request(new HttpPost(uri), payload);
    }

    /**
     * Executes an HTTP POST with the given URI and payload.
     *
     * At least one JIRA REST endpoint expects malformed JSON. The payload
     * argument is quoted and sent to the server with the application/json
     * Content-Type header. You should not use this function when proper JSON
     * is expected.
     *
     * @see https://jira.atlassian.com/browse/JRA-29304
     *
     * @param uri Full URI of the remote endpoint
     * @param payload Raw string to send to the remote service
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     */
    public JSONObject post(URI uri, String payload) throws RestException, IOException {
    	String quoted = null;
    	if(payload != null && !payload.equals(new JSONObject())){
    		quoted = String.format("\"%s\"", payload);
    	}
        return request(new HttpPost(uri), quoted);
    }

    /**
     * Executes an HTTP POST with the given path and payload.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     * @param payload JSON-encoded data to send to the remote service
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */
    public JSONObject post(String path, JSONObject payload)
        throws RestException, IOException, URISyntaxException {

        return post(buildURI(path), payload);
    }
    
    /**
     * Executes an HTTP POST with the given path.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */
    public JSONObject post(String path)
        throws RestException, IOException, URISyntaxException {
    	
        return post(buildURI(path), new JSONObject());
    }
    
    /**
     * Executes an HTTP POST with the given path and file payload.
     * 
     * @param uri Full URI of the remote endpoint
     * @param file java.io.File
     * 
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws RestException 
     */
    public JSONObject post(String path, File file) throws RestException, IOException, URISyntaxException{
        return request(new HttpPost(buildURI(path)), file);
    }

    /**
     * Executes an HTTP PUT with the given URI and payload.
     *
     * @param uri Full URI of the remote endpoint
     * @param payload JSON-encoded data to send to the remote service
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     */
    public JSONObject put(URI uri, JSONObject payload) throws RestException, IOException {
        return request(new HttpPut(uri), payload);
    }

    /**
     * Executes an HTTP PUT with the given path and payload.
     *
     * @param path Path to be appended to the URI supplied in the construtor
     * @param payload JSON-encoded data to send to the remote service
     *
     * @return JSON-encoded result or null when there's no content returned
     *
     * @throws RestException when an HTTP-level error occurs
     * @throws IOException when an error reading the response occurs
     * @throws URISyntaxException when an error occurred appending the path to the URI
     */

    public JSONObject put(String path, JSONObject payload)
        throws RestException, IOException, URISyntaxException {

        return put(buildURI(path), payload);
    }
    
    /**
     * Exposes the http client.
     *
     * @return the httpClient property
     */
    public HttpClient getHttpClient(){
        return this.httpClient;
    }
}

