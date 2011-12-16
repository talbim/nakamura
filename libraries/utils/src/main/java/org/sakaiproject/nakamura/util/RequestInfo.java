/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.util;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.util.Hashtable;
import java.util.Iterator;

import java.io.UnsupportedEncodingException;

import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * Class used to hold information about a request. eg: type, parameters, url..
 */
public class RequestInfo {

  private String url;
  private String method;
  private Hashtable<String, String[]> parameters;

  public RequestInfo(String url, Hashtable<String, String[]> parameters) throws MalformedURLException {
    setUrl(url);
    setParameters(parameters);
  }

  /**
   * Set a default requestinfo object.
   */
  public RequestInfo() {
    setParameters(new Hashtable<String, String[]>());
  }

  /**
   * Get a RequestInfo object created from a JSON block. This json object has to be in the
   * form of
   * 
   * <pre>
   * [
   * {
   *   "url" : "/foo/bar",
   *   "method" : "POST",
   *   "parameters\" : {
   *     "val" : 123,
   *     "val@TypeHint" : "Long"
   *   }
   * },
   * {
   *   "url" : "/_user/a/ad/admin/public/authprofile.json",
   *   "method" : "GET"
   * }
   * ]
   * </pre>
   * 
   * @param obj
   *          The JSON object containing the information to base this RequestInfo on.
   * @throws JSONException
   *           The JSON object could not be interpreted correctly.
   * @throws MalformedURLException
   */
  public RequestInfo(JSONObject obj) throws JSONException, MalformedURLException, UnsupportedEncodingException, URISyntaxException {
    setUrl(obj.getString("url"));
    setMethod(obj.getString("method"));
    setParameters(new Hashtable<String, String[]>());

    String queryString = new URI(getUrl()).getRawQuery();
    if (queryString != null) {
      // KERN-2095: If the URL we've been handed has query parameters, use them.
      // Note that parameters provided in the "parameters" block will override
      // these if both are present.
      for (String pair : queryString.split("&")) {
        String[] param = pair.split("=", 2);
        if (param.length == 2) {
          getParameters().put(URLDecoder.decode(param[0], "UTF-8"),
                              new String[] { URLDecoder.decode(param[1], "UTF-8") });
        }
      }
    }

    if (obj.has("parameters")) {

      JSONObject data = obj.getJSONObject("parameters");

      Iterator<String> keys = data.keys();

      while (keys.hasNext()) {
        String k = keys.next();
        Object val = data.get(k);
        if (val instanceof JSONArray) {
          JSONArray arr = (JSONArray) val;
          String[] par = new String[arr.length()];
          for (int i = 0; i < arr.length(); i++) {
            par[i] = arr.getString(i);
          }
          getParameters().put(k, par);
        } else {
          String[] par = { val.toString() };
          getParameters().put(k, par);
        }
      }
    }

  }

  /**
   * @param url
   *          The url where to fire a request on.
   * @throws MalformedURLException
   */
  public void setUrl(String url) throws MalformedURLException {
    if (!StringUtils.containsOnlySafeChars(url)) {
      throw new MalformedURLException("Invalid Character in URL request "+url);
    };
    this.url = url;
  }

  /**
   * @return The to fire a request to.
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param parameters
   *          The table that contains the key-values for the parameters.
   */
  public void setParameters(Hashtable<String, String[]> parameters) {
    this.parameters = parameters;
  }

  public Hashtable<String, String[]> getParameters() {
    return parameters;
  }

  /**
   * @param method
   *          the method to set
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  public boolean isSafe() {
    return ("GET|HEAD".indexOf(method) >= 0);
  }

}
