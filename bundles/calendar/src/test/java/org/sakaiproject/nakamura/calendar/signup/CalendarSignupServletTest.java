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
package org.sakaiproject.nakamura.calendar.signup;

import com.google.common.collect.ImmutableMap;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.PARTICIPANTS_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.RepositoryImpl;

import java.io.IOException;

import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CalendarSignupServletTest {

  private CalendarSignupServlet servlet;
  private String signupPath;
  private JackrabbitSession session;
  private String userName;
  private MockValue pathValue;
  private SlingRepository slingRepository;
  private Repository repository;

  @Before
  public void setUp() throws Exception {
    // re-implement for sparse
  }

  @Test
  public void testAnon() throws ServletException, IOException {
    // reimplement for sparse
  }

  @Test
  public void testSignedupAlready() throws Exception {
    // reimplement for sparse    
  }

  @Test
  public void testHandleSignup() throws Exception {
    // reimplement for sparse
  }

}
