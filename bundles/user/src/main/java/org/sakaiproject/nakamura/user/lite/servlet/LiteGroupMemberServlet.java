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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Also, when KERN-949 is fixed, we should change the getManagers() method.
 *
 *
 * Provides a listing for the members and managers of this group.
 */
@ServiceDocumentation(
  name = "Group Member Servlet", okForVersion = "1.1",
  shortDescription = "Provides a listing for the members and managers of this group.",
  description = "Provides a listing for the members and managers of this group.",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = { "sparse/group" },
      selectors = {
        @ServiceSelector(name = "members", description = "Binds to the members selector."),
        @ServiceSelector(name = "managers", description = "Binds to the managers selector."),
        @ServiceSelector(name = "everyone", description = "Binds to the everyone selector (members + managers)."),
        @ServiceSelector(name = "detailed", description = "Binds to the details selector.")
      },
      extensions = @ServiceExtension(name = "json", description = "javascript object notation")
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = "Create an external repository document.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 204, description = "Group doesn't exist."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    )
  }
)
@SlingServlet(resourceTypes = { "sparse/group" }, methods = { "GET" }, selectors = {
    "members", "managers", "everyone" }, extensions = { "json" })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Renders the members or managers for a group") })
public class LiteGroupMemberServlet extends SlingSafeMethodsServlet {

  private static final Logger logger = LoggerFactory.getLogger(LiteGroupMemberServlet.class);
  private static final long serialVersionUID = 7976930178619974246L;


  static final String ITEMS = "items";
  static final String PAGE = "page";
  @Reference
  private BasicUserInfoService basicUserInfoService;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.setCharacterEncoding("UTF-8");

    Authorizable authorizable = null;
    Resource resource = request.getResource();
    if (resource != null) {
      authorizable = resource.adaptTo(Authorizable.class);
    }

    if (!(authorizable instanceof Group)) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find group");
      return;
    }

    Group group = (Group) authorizable;

    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    writer.setTidy(selectors.contains("tidy"));

    // Get the sorting order, default is ascending or the natural sorting order (which is
    // null for a TreeMap.)
    Comparator<String> comparator = null;
    String order = "ascending";
    if (request.getRequestParameter("sortOrder") != null) {
      order = request.getRequestParameter("sortOrder").getString();
      if (order.equals("descending")) {
        comparator = Collections.reverseOrder();
      }
    }

    try {
      response.setContentType("application/json");
      TreeMap<String, Authorizable> map = null;
      if (selectors.contains("everyone")) {
        map = getMembers(request, group, comparator);
        map.putAll(getManagers(request, group, comparator));
      } else if (selectors.contains("managers")) {
        map = getManagers(request, group, comparator);
      } else {
        // Members is the default.
        map = getMembers(request, group, comparator);
      }

      // Do some paging.
      long items = (request.getParameter(ITEMS) != null) ? Long.parseLong(request
          .getParameter(ITEMS)) : 25;
      long page = (request.getParameter(PAGE) != null) ? Long.parseLong(request
          .getParameter(PAGE)) : 0;
      if (page < 0) {
        page = 0;
      }
      if (items < 0) {
        items = 25;
      }
      Iterator<Entry<String, Authorizable>> iterator = getInPlaceIterator(request, map,
          items, page);

      // Write the whole lot out.
      writer.array();
      int i = 0;
      while (iterator.hasNext() && i < items) {
        Entry<String, Authorizable> entry = iterator.next();
        Authorizable au = entry.getValue();
        ValueMap profile = new ValueMapDecorator(basicUserInfoService.getProperties(au));
        if (profile != null) {
          writer.valueMap(profile);
          i++;
        } /*else {
          // profile wasn't found.  safe to ignore and not include the group
          logger.info("Profile not found for " + au.getId());
        }*/
      }
      writer.endArray();

    } catch (JSONException e) {
      logger.error(e.getMessage(),e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to build a proper JSON output.");
      return;
    } catch (StorageClientException e) {
      logger.error(e.getMessage(),e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
      "Failed to get members.");
      return;
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage());
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
      "Failed to get members.");
      return;
    }/* catch (RepositoryException e) {
      logger.error(e.getMessage(),e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
      "Failed to get members.");
      return;
    }*/

  }

  /**
   * @param request
   * @param map
   * @return
   */
  private Iterator<Entry<String, Authorizable>> getInPlaceIterator(
      SlingHttpServletRequest request, TreeMap<String, Authorizable> map,
      long items, long page) {
    Iterator<Entry<String, Authorizable>> iterator = map.entrySet().iterator();
    long skipNum = items * page;

    while (skipNum > 0) {
      iterator.next();
      skipNum--;
    }

    return iterator;
  }

  /**
   * @param request
   * @param group
   * @param writer
   * @throws RepositoryException
   * @throws JSONException
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  protected TreeMap<String, Authorizable> getMembers(SlingHttpServletRequest request,
      Group group, Comparator<String> comparator) throws
      JSONException, AccessDeniedException, StorageClientException {
    TreeMap<String, Authorizable> map = new TreeMap<String, Authorizable>(comparator);

    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    AuthorizableManager authorizableManager = session.getAuthorizableManager();

    // Only the direct members are required.
    String[] members = group.getMembers();
    List<String> managers = Arrays.asList(StorageClientUtils.nonNullStringArray((String[]) group.getProperty(UserConstants.PROP_GROUP_MANAGERS)));
    for ( String memberName : members) {
      Authorizable member = null;
      try {
        member = authorizableManager.findAuthorizable(memberName);
      } catch (AccessDeniedException e) {
        // this means a group has a member we're not allowed to see
        // that's normal, and we'll just skip over it. (KERN-2302)
        continue;
      }
      // filter this out if it is a manager member
      if (member != null && !managers.contains(memberName)) {
        String name = getName(member);
        map.put(name, member);
      }
    }
    return map;
  }

  /**
   * KERN-1026 changed the results of this to be the authz's that are members of the
   * managers group associated to a group rather than the group managers associated to the
   * group.
   * <p>
   * <del>Get the managers for a group. These should be stored in the
   * {@link UserConstants#PROP_GROUP_MANAGERS}.</del>
   *
   * @param request
   * @param group
   * @param writer
   * @throws RepositoryException
   * @throws JSONException
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  protected TreeMap<String, Authorizable> getManagers(SlingHttpServletRequest request,
      Group group, Comparator<String> comparator) throws
      JSONException, StorageClientException, AccessDeniedException {
    TreeMap<String, Authorizable> map = new TreeMap<String, Authorizable>(comparator);

    // KERN-949 will probably change this.
    // note above was made before this was changed to retrieving members of the managers
    // group and may not apply.
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    AuthorizableManager authorizableManager = session.getAuthorizableManager();

    String[] members = group.getMembers();
    List<String> managers = Arrays.asList(StorageClientUtils.nonNullStringArray((String[]) group.getProperty(UserConstants.PROP_GROUP_MANAGERS)));
    for  (String memberName : members) {
      Authorizable mau = null;
      try {
        mau = authorizableManager.findAuthorizable(memberName);
      } catch (AccessDeniedException e) {
        // this means a group has a member we're not allowed to see
        // that's normal, and we'll just skip over it. (KERN-2302)
        continue;
      }
      if (mau != null && managers.contains(memberName)) {
        String name = getName(mau);
        map.put(name, mau);
      }
    }
    return map;

  }

  /**
   * Get's the name for an authorizable on what the list should be sorted.
   * sakai:group-title for Groups, lastName for Users.
   *
   * @param member
   *          The authorizable to get a name for.
   * @return The name.
   * @throws RepositoryException
   */
  private String getName(Authorizable member)  {
    String name = member.getId();
    if (member instanceof Group) {
      name = (String) member.getProperty("sakai:group-title");
    } else {
      name = (String) member.getProperty("lastName");
    }
    // We need to add the ID to keep the keys unique. (ieb, ???? name depends on member, so this does not keep the name unique)
    return name + member.getId();
  }

}
