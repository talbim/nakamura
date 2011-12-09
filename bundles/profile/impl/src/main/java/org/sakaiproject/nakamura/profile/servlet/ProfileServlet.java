/*
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
package org.sakaiproject.nakamura.profile.servlet;


import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProfileConstants;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Creates a REST endpoint for the profile service.
 */
@ServiceDocumentation(bindings = {
  @ServiceBinding(type = BindingType.TYPE,
    bindings = {
    ProfileConstants.GROUP_PROFILE_RT, ProfileConstants.USER_PROFILE_RT },
    extensions = { @ServiceExtension(name = "json", description = "json format")
    })
  },
  methods = {
    @ServiceMethod(name = "GET", description = "Responds to simple GET method requests",
      response = {
        @ServiceResponse(code = 200, description = "Responds with a 200 if the request was successful, the output is a json "
          + "tree of the profile with external references expanded."),
        @ServiceResponse(code = 404, description = "Responds with a 404 is the profile node cant be found, body contains no output"),
        @ServiceResponse(code = 403, description = "Responds with a 403 if the user does not have permission to access the profile or part of it"),
        @ServiceResponse(code = 500, description = "Responds with a 500 on any other error")
      })
  },
  name = "Profile Servlet", okForVersion = "1.1",
  shortDescription = "Displays the JSON rendering of a user or group profile.",
  description = {
    "This servlet provides a fully rendered profile in json form, when the resource the URL points to is a profile node. If there are any external"
        + "references in the sub tree of child nodes underneath the resource, those nodes will be expanded by invoking the appropriate Profile Provider."
        + "If no ProfileProvider can be found for an external node, then all the properties will be sent in json form just as for a internal node.",
    "This servlet relies on the ProfileService implementation that uses the sub path of a sub node to determining the type of profile information. That"
        + " is used to lookup settings for the node, including a ProfileProvider instance name and a path to additional configuration settings for that profile "
        + " provider. All of that information is used by the ProfileProvider implementation to convert and node in the profile subtree, marked as external "
        + "into a map of maps, the structure and layout of that map being determined by the implementation of the ProfileProvider. "
  })
@SlingServlet(extensions = { "json" }, methods = { "GET" }, selectors = "profile", resourceTypes = {
    ProfileConstants.GROUP_PROFILE_RT, ProfileConstants.USER_PROFILE_RT })
public class ProfileServlet extends SlingSafeMethodsServlet {

  private static final String TIDY = "tidy";

  /**
   *
   */
  private static final long serialVersionUID = -8608846717806111472L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServlet.class);

  @Reference
  protected ProfileService profileService;

  @Reference
  private ConnectionManager connMgr;
  
  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    Resource resource = request.getResource();
    Content profileContent = resource.adaptTo(Content.class);
    javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session = StorageClientUtils.adaptToSession(jcrSession);
    String currUser = request.getRemoteUser();
    try {
      ValueMap map = profileService.getProfileMap(profileContent, jcrSession);
      String profileUserId = map.get("userid", String.class);
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(isTidy(request));
      writer.object();
      ExtendedJSONWriter.writeValueMapInternals(writer, map);
      connMgr.writeConnectionInfo(writer, session, currUser, profileUserId);
      writer.endObject(); 
    } catch (AccessDeniedException e) {
      LOGGER.warn("Failed to access profile at {}: {}", new Object[] {
          resource.getPath(), e.getMessage(), e });
      response.reset();
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    } catch (JSONException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new ServletException(e.getMessage(), e);
    }
  }

  private boolean isTidy(SlingHttpServletRequest req) {
    for (String selector : req.getRequestPathInfo().getSelectors()) {
      if (TIDY.equals(selector)) {
        return true;
      }
    }
    return false;
  }
}
