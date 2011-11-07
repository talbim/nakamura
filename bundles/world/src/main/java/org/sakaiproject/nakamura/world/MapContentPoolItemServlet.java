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
package org.sakaiproject.nakamura.world;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
@ServiceDocumentation(name = "MapContentPoolItemServlet documentation", okForVersion = "0.11",
  shortDescription = "Create a token on the requested piece of content, and update permissions on a specified target piece of content.",
  description = "Create a token on the requested piece of content, and update permissions on a specified target piece of content.",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE,
      bindings = "sparse/Content",
      selectors = @ServiceSelector(name = "map-pool", description = "Binds to the map-pool selector."))
    },
  methods = {
    @ServiceMethod(name = "POST", description = "Creates a token on the requested content. Returns JSON with the newly created tokenPrincipalId.",
      parameters = {
        @ServiceParameter(name = "src", description = "The path to the target content whose permission will be updated"),
        @ServiceParameter(name = "grant", description = "Multi-valued. The permissions to grant."),
        @ServiceParameter(name = "deny", description = "Multi-valued. The permissions to deny.")
      },
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_FORBIDDEN, description = "Insufficient permission to create the token and update permissions on the target."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
@SlingServlet(resourceTypes = { "sparse/Content" }, selectors = { "map-pool" }, methods = { "POST" })
public class MapContentPoolItemServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 5558859864475232589L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MapContentPoolItemServlet.class);

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Content content = request.adaptTo(Content.class);
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
          .adaptTo(javax.jcr.Session.class));
      AccessControlManager accessControlManager = session.getAccessControlManager();
      ContentManager contentManager = session.getContentManager();
      String targetContentPath = request.getParameter("src");
      String[] granted = request.getParameterValues("grant");
      String[] denied = request.getParameterValues("deny");

      int grantedBitmap = buildPermissionBitmap(granted);
      int deniedBitmap = buildPermissionBitmap(denied);
      String aclID = Integer.toHexString(grantedBitmap) + "_"
          + Integer.toHexString(deniedBitmap);
      String tokenPrincipal = AccessControlManager.DYNAMIC_PRINCIPAL_STEM + aclID;

      accessControlManager.setAcl(
          Security.ZONE_CONTENT,
          targetContentPath,
          new AclModification[] {
              new AclModification(AclModification.grantKey(tokenPrincipal),
                  grantedBitmap, Operation.OP_REPLACE),
              new AclModification(AclModification.denyKey(tokenPrincipal), deniedBitmap,
                  Operation.OP_REPLACE), });

      String tokenPath = StorageClientUtils.newPath(content.getPath(), tokenPrincipal);
      Content token = null;
      if (contentManager.exists(tokenPath)) {
        token = contentManager.get(tokenPath);
      } else {
        token = new Content(tokenPath, null);
      }
      accessControlManager.signContentToken(token, targetContentPath);
      contentManager.update(token);

      response.sendError(HttpServletResponse.SC_OK);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Map<String, Object> responseMap = ImmutableMap.of("tokenPrincipalId", (Object) tokenPrincipal);
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      ExtendedJSONWriter.writeValueMap(jsonWriter, responseMap);


    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    }
  }

  private int buildPermissionBitmap(String[] granted) {
    int bitmap = 0;
    for (String permission : granted) {
      bitmap = bitmap | Permissions.parse(permission.toLowerCase()).getPermission();
    }
    return bitmap;
  }

}
