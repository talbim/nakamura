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
package org.sakaiproject.nakamura.http.usercontent;

import static org.apache.sling.jcr.resource.JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.sakaiproject.nakamura.api.auth.trusted.TokenTrustValidator;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;

import java.io.IOException;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests to transfer user authN from a trusted host to a non content host.
 */
@Component(immediate = true, metatype = true, enabled=true)
@Service(value=AuthenticationHandler.class)
@Properties( value={
    @Property(name="service.description",value="User Content Authentication Handler"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name=AuthenticationHandler.PATH_PROPERTY, value="/" ),
    @Property(name=AuthenticationHandler.TYPE_PROPERTY, value="UserContentAuthenticationHandler" )
})
public class UserContentAuthenticationHandler implements AuthenticationHandler, TokenTrustValidator {

  private static final String USER_CONTENT_AUTH = UserContentAuthenticationHandler.class.getName();

  static final String TRANSFER_CREDENTIAL_MARKER = UserContentToken.class.getName();

  @Reference
  protected ServerProtectionService serverProtectionService;
  
  @Reference
  protected TrustedTokenService trustedTokenService;
  
  public void dropCredentials(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
  }

  @Activate
  public void activate(Map<String, Object> properties) {
    trustedTokenService.registerType(UserContentAuthenticationTokenServiceWrapper.TYPE, this);
  }

  @Activate
  public void deactivate(Map<String, Object> properties) {
    trustedTokenService.deregisterType(UserContentAuthenticationTokenServiceWrapper.TYPE, this);
  }

  public AuthenticationInfo extractCredentials(HttpServletRequest request,
      HttpServletResponse response) {
    String userId = serverProtectionService.getTransferUserId(request);
    if ( userId != null ) {
      AuthenticationInfo authenticatioInfo = new AuthenticationInfo(USER_CONTENT_AUTH);
      SimpleCredentials credentials = new SimpleCredentials(userId, new char[0]);
      credentials.setAttribute(TRANSFER_CREDENTIAL_MARKER, new UserContentToken(userId));
      authenticatioInfo.put(AUTHENTICATION_INFO_CREDENTIALS, credentials);
      // the credentials are validated by a LoginPlugin, but we can trust them already.
      
      // ensure a trusted token is added here for subsequent requests
      UserContentAuthenticationTokenServiceWrapper userContentAuthenticationTokenService = new UserContentAuthenticationTokenServiceWrapper(
          this, trustedTokenService);
      userContentAuthenticationTokenService.addToken(request, response);
      
      return authenticatioInfo;
    }
    return null;
  }

  public boolean requestCredentials(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    return false;
  }

  /**
   * Once the token is issued it will only be trusted to perform GET and HEAD operations.
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.auth.trusted.TokenTrustValidator#isTrusted(javax.servlet.http.HttpServletRequest)
   */
  public boolean isTrusted(HttpServletRequest request) {
    String method = request.getMethod();
    return ( "GET".equals(method) || "HEAD".equals(method));
  }

}
