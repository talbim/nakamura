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
package org.sakaiproject.nakamura.user.lite.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_VIEWERS;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.resource.RequestProperty;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Operation implementation for updating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a group's properties. Maps on to nodes of resourceType
 * <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups/ae/3f/ed/testGroup</code> mapped to a
 * resource url <code>/system/userManager/group/testGroup</code>. This servlet responds at
 * <code>/system/userManager/group/testGroup.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>The property is deleted, eg prop1@Delete</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group's resource locator. The redirect comes
 * with HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/group/testGroup.update.html
 * </code>
 *
 *
 */
@SlingServlet(resourceTypes={"sparse/group"}, methods={"POST"}, selectors={"update"})
@Properties( value={@Property(name="servlet.post.dateFormats",
             value={"EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd",
                    "dd.MM.yyyy HH:mm:ss",
                   "dd.MM.yyyy"}
 )})
@ServiceDocumentation(name = "Update Group Servlet", okForVersion = "0.11",
    description = "Updates a group's properties. Maps on to nodes of resourceType sparse/group "
      + "like a:math101 mapped to a resource "
      + "url /system/userManager/group/math101. This servlet responds at "
      + "/system/userManager/group/math101.update.html",
    shortDescription = "Update a group properties",
    bindings = {
      @ServiceBinding(type = BindingType.TYPE,
        bindings = { "sparse/group" },
        selectors = @ServiceSelector(name = "update", description = "Updates the properties of a group"),
        extensions = @ServiceExtension(name = "html", description = "Posts produce html containing the update status"))
    },
    methods = {
      @ServiceMethod(name = "POST",
        description = {
          "Updates a group setting or deleting properties, "
            + "storing additional parameters as properties of the group.",
          "Example<br>"
            + "<pre>curl -Fproperty1@Delete -Fproperty2=value2 http://localhost:8080/system/userManager/group/math101.update.html</pre>"
        },
        parameters = {
        @ServiceParameter(name = "propertyName@Delete", description = "Delete property, eg property1@Delete means delete property1 (optional)"),
        @ServiceParameter(name = ":member", description = "Add a member to this group (optional)"),
        @ServiceParameter(name = ":member@Delete", description = "Remove a member from this group (optional)"),
        @ServiceParameter(name = ":manager", description = "Add a manager to this group, note: this does not add the manager as a member! (optional)"),
        @ServiceParameter(name = ":manager@Delete", description = "Remove a manager from this group, note: this does not remove the manager as a member! (optional)"),
        @ServiceParameter(name = ":viewer", description = "Add a viewer to this group, note: this does not add the viewer as a member! (optional)"),
        @ServiceParameter(name = ":viewer@Delete", description = "Remove a viewer from this group, note: this does not remove the viewer as a member! (optional)"),
        @ServiceParameter(name = "propertyName@Delete", description = "Delete property, eg property1@Delete means delete property1 (optional)"),
        @ServiceParameter(name="",description="Additional parameters become group node properties, " +
          "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
      }, response={
        @ServiceResponse(code = 200, description = "Success, a redirect is sent to the group's resource locator with HTML describing status."),
        @ServiceResponse(code = 404, description = "Group was not found."),
        @ServiceResponse(code = 500, description = "Failure with HTML explanation.") })
    })
public class LiteUpdateSakaiGroupServlet extends LiteAbstractSakaiGroupPostServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2378929115784007976L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteUpdateSakaiGroupServlet.class);

  /**
   * The post processor service.
   *
   */
  @Reference
  protected transient LiteAuthorizablePostProcessService postProcessorService;

  /**
   * The JCR Repository we access to resolve resources
   *
   */
  @Reference
  private transient Repository repository;

  /**
   * Used to launch OSGi events.
   *
   */
  @Reference
  protected transient EventAdmin eventAdmin;

  /** Returns the JCR repository used by this service. */
  @Override
  protected Repository getRepository() {
    return repository;
  }

  /**
   * {@inheritDoc}
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   *
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse htmlResponse, List<Modification> changes) throws AccessDeniedException, StorageClientException  {

    Authorizable authorizable = null;
    Resource resource = request.getResource();

    if (resource != null) {
      authorizable = resource.adaptTo(Authorizable.class);
    }

    // check that the group was located.
    if (authorizable == null) {
      throw new ResourceNotFoundException("Group to update could not be determined");
    }
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    if (session == null) {
      throw new StorageClientException("Sparse Session not found");
    }

    // let's check permission right up front. KERN-2143
    AccessControlManager accessControlManager = session.getAccessControlManager();
    Authorizable currentUser = session.getAuthorizableManager().findAuthorizable(session.getUserId());
    if (currentUser == null || !accessControlManager.can(currentUser, "AU", authorizable.getId(), Permissions.CAN_WRITE)) {
      htmlResponse.setStatus(HttpServletResponse.SC_FORBIDDEN, "No permission to update authorizable:" + authorizable.getId());
      return;
    }

    String groupPath = LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
    + authorizable.getId();

    Map<String, RequestProperty> reqProperties = collectContent(request, htmlResponse, groupPath);
    // cleanup any old content (@Delete parameters)
    // This is the only way to make a private group (one with a "rep:group-viewers"
    // property) no longer private.
    Map<String, Object> toSave = new HashMap<String, Object>() {
      /**
       * 
       */
      private static final long serialVersionUID = 726298727259672826L;

      @Override
      public Object put(String key, Object object) {
        if ( containsKey(key) && object != get(key)) {
          LOGGER.warn("Overwriting existing object to save, may loose data key:{} old:{} new:{} ",new Object[]{key,get(key),object});
        }
        return super.put(key, object);
      }
    };
    
    processDeletes(authorizable, reqProperties, changes, toSave);

    // It is not allowed to touch the rep:group-managers and rep:group-viewers
    // properties directly except to delete them.
    reqProperties.remove(groupPath + "/" + PROP_GROUP_MANAGERS);
    reqProperties.remove(groupPath + "/" + PROP_GROUP_VIEWERS);

    // write content from form
    writeContent(session, authorizable, reqProperties, changes, toSave);

    // update the group memberships
    
    dumpToSave(toSave, "after write content");

    if (authorizable instanceof Group) {
      updateGroupMembership(request, session, authorizable, changes, toSave);
      dumpToSave(toSave, "after updateGroup membership");
      updateOwnership(request, (Group)authorizable, new String[0], changes, toSave);
    }
    dumpToSave(toSave, "before save");
      
    saveAll(session, toSave);

    try {
      postProcessorService.process(authorizable, session, ModificationType.MODIFY, request);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);

      htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
      return;
    }

    // Launch an OSGi event for updating a group.
    try {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, authorizable.getId());
      properties.put("path", authorizable.getId());
      EventUtils
          .sendOsgiEvent(properties, UserConstants.TOPIC_GROUP_UPDATE, eventAdmin);
    } catch (Exception e) {
      // Trap all exception so we don't disrupt the normal behaviour.
      LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
    }
    
  }

  /**
   * @param slingRepository
   */
  protected void bindRepository(Repository repository) {
    this.repository = repository;

  }

  /**
   * @param slingRepository
   */
  protected void unbindRepository(Repository repository) {
    this.repository = null;

  }

}
