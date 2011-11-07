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
package org.sakaiproject.nakamura.docproxy;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY_DOCUMENT;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.EXTERNAL_ID;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class DocumentProxyServletTest extends AbstractDocProxyServlet {

  private ExternalDocumentProxyServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    servlet = new ExternalDocumentProxyServlet();
    servlet.activate(componentContext);
    servlet.tracker = tracker;
  }

  @After
  public void tearDown() {
    servlet.deactivate(componentContext);
  }

  @Test
  public void testGet() throws ServletException, IOException, PathNotFoundException,
      RepositoryException, JSONException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    // Session
    //    expect(session.getItem("/docproxy/disk/README"))
    //        .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk/READMEproxy")).andReturn(documentNode);
    //    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn("zach");

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/READMEproxy");
    expect(request.getResourceResolver()).andReturn(resolver);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream stream = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    expect(response.getOutputStream()).andReturn(stream);
    replay();

    servlet.doGet(request, response);

    String result = baos.toString("UTF-8");
    assertEquals("K2 docProxy test resource", result);
  }

  @Test
  public void testDocumentGet() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException, IOException,
      ServletException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);


    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));

    // Session
    // change reflects change in servlet. READMEproxy proxies README
    expect(session.getItem("/docproxy/disk/READMEproxy")).andReturn(documentNode);
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
/*
    --- removed to match changed servlet

    expect(session.getNodeByIdentifier("proxyUUID")).andReturn(proxyNode);
*/
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getUserID()).andReturn("zach");

    // Request
    // change reflects change in servlet
    expect(request.getRequestURI()).andReturn("/docproxy/disk/READMEproxy");
    expect(request.getResourceResolver()).andReturn(resolver);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletOutputStream stream = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    expect(response.getOutputStream()).andReturn(stream);
    replay();

    servlet.doGet(request, response);

    String result = baos.toString("UTF-8");
    assertEquals("K2 docProxy test resource", result);
  }

  @Test
  public void testNoProcessor() throws PathNotFoundException, RepositoryException,
      ServletException, IOException {
    Session session = createMock(Session.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    // test changes reflect servlet changes
    // make a new test document proxy node
    Node  node = new MockNode("/docproxy/disk/TESTproxy");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_EXTERNAL_REPOSITORY_DOCUMENT);
    // set a bad repository type
    node.setProperty(REPOSITORY_PROCESSOR, "foo");
    // Session
    //    expect(session.getItem("/docproxy/disk/README"))
    //  .andThrow(new PathNotFoundException());
    expect(session.getItem("/docproxy/disk/TESTproxy")).andReturn(node);
    // expect(session.getItem("/docproxy/disk")).andReturn(proxyNode);
    expect(resolver.adaptTo(Session.class)).andReturn(session);

    // Request
    expect(request.getRequestURI()).andReturn("/docproxy/disk/TESTproxy");
    expect(request.getResourceResolver()).andReturn(resolver);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown repository.");

    replay();

    servlet.doGet(request, response);
  }

}
