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
package org.sakaiproject.nakamura.presence.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Search result processor to write out profile information when search returns home nodes
 * (sakai/user-home). This result processor should live in the user bundle but at the time
 * of this writing, moving to that bundle creates a cyclical dependency of:<br/>
 * search -&gt; personal -&gt; user -&gt; search
 *
 * TODO: Sort the above out and move this code to the correct bundle.
 */
@Component
@Service
@Properties({
  @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
  @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "Profile")
})
public class ProfileNodeSearchResultProcessor implements SolrSearchResultProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileNodeSearchResultProcessor.class);

  @Reference
  protected SolrSearchServiceFactory searchServiceFactory;

  @Reference
  protected ProfileService profileService;

  @Reference
  protected PresenceService presenceService;

  public ProfileNodeSearchResultProcessor() {
  }

  ProfileNodeSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory,
      ProfileService profileService, PresenceService presenceService) {
    if (searchServiceFactory == null || profileService == null || presenceService == null) {
      throw new IllegalArgumentException(
          "SearchServiceFactory, ProfileService and PresenceService must be set when not using as a component");
    }
    this.searchServiceFactory = searchServiceFactory;
    this.presenceService = presenceService;
    this.profileService = profileService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    // return the result set
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.search.processors.SearchResultProcessor#writeNode(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.Row)
   */
  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result) throws JSONException {
    writeResult(request, write, result, false);
  }

  public void writeResult(SlingHttpServletRequest request, JSONWriter write,
      Result result, boolean objectInProgress) throws JSONException {
    javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session = StorageClientUtils.adaptToSession(jcrSession);

    try {
      AuthorizableManager authMgr = session.getAuthorizableManager();

      String authorizableId = (String) result.getFirstValue("path");
      Authorizable auth = authMgr.findAuthorizable(authorizableId);

      if (!objectInProgress) {
        write.object();
      }
      if (auth != null) {
        ValueMap map = profileService.getProfileMap(auth, jcrSession);
        ExtendedJSONWriter.writeValueMapInternals(write, map);

        // If this is a User Profile, then include Presence data.
        if (!auth.isGroup()) {
          PresenceUtils.makePresenceJSON(write, authorizableId, presenceService, true);
        }
      }
      if (!objectInProgress) {
        write.endObject();
      }
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
