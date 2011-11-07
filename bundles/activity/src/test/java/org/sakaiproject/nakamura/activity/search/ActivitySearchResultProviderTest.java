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
package org.sakaiproject.nakamura.activity.search;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.junit.Test;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class ActivitySearchResultProviderTest extends AbstractEasyMockTest {

  @Test
  public void testLoadProperties() throws RepositoryException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("admin");

    JackrabbitSession session = createMock(JackrabbitSession.class);
    Authorizable admin = createAuthorizable("admin", false, true);
    UserManager um = createUserManager(null, true, admin);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(session.getUserManager()).andReturn(um);
    expect(request.getResourceResolver()).andReturn(resolver);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    replay();
    ActivitySearchPropertyProvider provider = new ActivitySearchPropertyProvider();
    Map<String, String> propertiesMap = new HashMap<String, String>();
    provider.loadUserProperties(request, propertiesMap);
    String actual = propertiesMap.get("_myFeed");
    String expected = ClientUtils.escapeQueryChars(LitePersonalUtils.PATH_AUTHORIZABLE
        + "admin/private/" + ActivityConstants.ACTIVITY_FEED_NAME);
    assertEquals(expected, actual);
  }

  @Test
  public void testAnonLoadProperties() throws RepositoryException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("anonymous");

    JackrabbitSession session = createMock(JackrabbitSession.class);
    Authorizable anonymous = createAuthorizable("anonymous", false, true);
    UserManager um = createUserManager(null, true, anonymous);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(session.getUserManager()).andReturn(um);
    expect(request.getResourceResolver()).andReturn(resolver);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    replay();
    ActivitySearchPropertyProvider provider = new ActivitySearchPropertyProvider();
    Map<String, String> propertiesMap = new HashMap<String, String>();
    provider.loadUserProperties(request, propertiesMap);
    String actual = propertiesMap.get("_myFeed");
    String expected = ClientUtils.escapeQueryChars(LitePersonalUtils.PATH_AUTHORIZABLE
        + "anonymous/private/" + ActivityConstants.ACTIVITY_FEED_NAME);
    assertEquals(expected, actual);
  }

}
