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
package org.sakaiproject.nakamura.captcha;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.captcha.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Service
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "An implementation for the CaptchaService that uses the Google reCAPTCHA webservice.") })
public class ReCaptchaService implements CaptchaService {

  @Property(value = "6Lef4bsSAAAAAJOwQE-qwkAOzGG3DizFP7GYYng-", description = "Your public key for the reCAPTCHA service")
  static final String KEY_PUBLIC = "org.sakaiproject.nakamura.captcha.key_public";

  @Property(value = "6Lef4bsSAAAAAId09ufqqs89SwdWpa9t7htW1aRc", description = "Your private key for the reCAPTCHA service.")
  static final String KEY_PRIVATE = "org.sakaiproject.nakamura.captcha.key_private";

  @Property(value = "http://www.google.com/recaptcha/api/verify", description = "The REST endpoint for the reCAPTCHA service.")
  static final String RECAPTCHA_ENDPOINT = "org.sakaiproject.nakamura.captcha.endpoint";

  static final Logger LOGGER = LoggerFactory.getLogger(ReCaptchaService.class);

  /**
   * The HTTP client that we will use to connect to the reCAPTCHA REST service.
   */
  private HttpClient client;
  private String keyPrivate;
  private String keyPublic;
  private String endpoint;

  @Activate
  protected void activate(Map<?, ?> properties) {
    // Get the properties.
    keyPrivate = OsgiUtil.toString(properties.get(KEY_PRIVATE), "");
    keyPublic = OsgiUtil.toString(properties.get(KEY_PUBLIC), "");
    endpoint = OsgiUtil.toString(properties.get(RECAPTCHA_ENDPOINT), "");

    // Initialize the client on start up.
    client = new HttpClient();

    // allow communications via a proxy server if command line
    // java parameters http.proxyHost,http.proxyPort,http.proxyUser,
    // http.proxyPassword have been provided.
    String proxyHost = System.getProperty("http.proxyHost","");
    int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort","80"));
    if (!proxyHost.equals("") ) {
      // allow communications via a non-authenticating proxy
      client.getHostConfiguration().setProxy(proxyHost, proxyPort);

      String proxyUser = System.getProperty("http.proxyUser","");
      String proxyPassword = System.getProperty("http.proxyPassword","");
      if ( !proxyUser.equals("") ) {
        // allow communications via an authenticating proxy
        Credentials credentials = new UsernamePasswordCredentials(proxyUser, proxyPassword);
        AuthScope authScope = new AuthScope(proxyHost, proxyPort);
        client.getState().setProxyCredentials(authScope, credentials);
      }
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.captcha.CaptchaService#checkRequest(javax.servlet.http.HttpServletRequest)
   */
  public boolean checkRequest(HttpServletRequest request) {
    // We need the following things to verify a request with reCAPTCHA
    // - Our private key (privatekey)
    // - The user his IP address (remoteip)
    // - The challenge (challenge)
    // - The response (response)

    String challenge = request.getParameter(":recaptcha-challenge");
    String response = request.getParameter(":recaptcha-response");

    // No point in doing a request when this one is false.
    if (challenge == null || response == null) {
      return false;
    }

    PostMethod method = new PostMethod(endpoint);
    method.addParameter("privatekey", keyPrivate);
    method.addParameter("remoteip", request.getRemoteAddr());
    method.addParameter("challenge", challenge);
    method.addParameter("response", response);

    try {
      int code = client.executeMethod(method);
      if (code != 200) {
        LOGGER.warn("ReCaptcha request responded with {} {} if this persists enable debug logging on this class for more info ",code, method.getStatusText());
        if ( LOGGER.isDebugEnabled()) {
          LOGGER.debug("Response was {} ",method.getResponseBodyAsString());
        }
        return false;
      }

      InputStream bodyStream = method.getResponseBodyAsStream();
      StringWriter writer = new StringWriter();
      IOUtils.copy(bodyStream, writer);
      String body = writer.toString();

      LOGGER.debug("=== start of reCAPTCHA output ===\n" + body + "\n=== end of reCAPTCHA output ==="); // allow logging statement to show more clearly that the reCAPTCHA output contains a crlf character.
      if (!body.startsWith("true")) {
        LOGGER.warn("ReCaptcha challenge [{}] responde [{}] denied, debug level has more info ",challenge,response);
        return false;
      }

      return true;
    } catch (HttpException e) {
      LOGGER
          .error(
              "Caught an HTTPException when trying to check a request with the reCAPTCHA service.",
              e);
    } catch (IOException e) {
      LOGGER
          .error(
              "Caught an IOException when trying to check a request with the reCAPTCHA service.",
              e);
    }

    return false;
  }

  /**
   * @return The properties that are defined in the OSGi admin console.
   */
  public Map<String, Object> getProperties() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("public-key", keyPublic);
    return map;
  }

}
