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
package org.sakaiproject.nakamura.connections.search;

import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.DEFAULT_PAGED_ITEMS;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_ITEMS_PER_PAGE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * <pre>
 * KERN-1799
 * Create a feed that lists groups related to My Groups. The criteria that 
 * should be used for this are: 
 * 
 * - Groups that my contacts are a member of 
 * - Groups with tags or title words similar to My Groups 
 * 
 * The feed should not include groups that I'm already a member of. 
 * 
 * When less than 11 items are found for these criteria, the feed should be 
 * filled up with random groups. However, preference should be given to groups 
 * that have a profile picture, and a high number of participants and content 
 * items.
 * </pre>
 */
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "MyRelatedGroupsSearchBatchResultProcessor") })
@Service(value = SolrSearchBatchResultProcessor.class)
public class MyRelatedGroupsSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  /**
   * "These go to eleven"
   */
  public static final int VOLUME = 11;

  protected static final String AUTHORIZABLE_RT = "authorizable";

  private static final Logger LOG = LoggerFactory
      .getLogger(MyRelatedGroupsSearchBatchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  @Reference
  private ProfileService profileService;

  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&("
      + SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  private transient SolrSearchResultProcessor defaultSearchProcessor;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(final SlingHttpServletRequest request,
      final JSONWriter writer, final Iterator<Result> iterator) throws JSONException {
    long startTicks = System.currentTimeMillis();
    long firstQueryTicks = 0; long secondQueryTicks = 0;
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    final String user = session.getUserId();
    final long nitems = SolrSearchUtil.longRequestParameter(request,
        PARAMS_ITEMS_PER_PAGE, DEFAULT_PAGED_ITEMS);
    // TODO add proper paging support
    // final long page = SolrSearchUtil.longRequestParameter(request, PARAMS_PAGE, 0);

    final Set<String> processedGroups = new HashSet<String>();
    try {
      /* first render search results */
      final AuthorizableManager authMgr = session.getAuthorizableManager();
      while (iterator.hasNext() && processedGroups.size() < nitems) {
        final Result result = iterator.next();
        final String path = (String) result.getFirstValue("path");
        final Group g = (Group) authMgr.findAuthorizable(path);
        renderAuthorizable(request, writer, g, processedGroups);
      }
      firstQueryTicks = System.currentTimeMillis();
      if (LOG.isDebugEnabled())
        LOG.debug("writeResults() first query processing took {} seconds",
            new Object[] { (float) (firstQueryTicks - startTicks) / 1000 });
      if (processedGroups.size() < nitems) {
        /* Not enough results, add some random groups per spec */
        final StringBuilder sourceQuery = new StringBuilder("resourceType:");
        sourceQuery.append(AUTHORIZABLE_RT);
        sourceQuery.append(" AND type:g AND -readers:");
        sourceQuery.append(ClientUtils.escapeQueryChars(user));

        final Iterator<Result> i = SolrSearchUtil.getRandomResults(request,
            defaultSearchProcessor, sourceQuery.toString(), "items",
            String.valueOf(VOLUME), "page", "0");

        if (i != null) {
          while (i.hasNext() && processedGroups.size() <= nitems) {
            final Result result = i.next();
            final String path = (String) result.getFirstValue("path");
            final Group g = (Group) authMgr.findAuthorizable(path);
            renderAuthorizable(request, writer, g, processedGroups);
          }
        }
      }
      secondQueryTicks = System.currentTimeMillis();
      if (LOG.isDebugEnabled())
        LOG.debug("writeResults() second query processing took {} seconds",
            new Object[] { (float) (secondQueryTicks - firstQueryTicks) / 1000 });
      if (processedGroups.size() < VOLUME) {
        LOG.info(
            "Did not meet functional specification. There should be at least {} results; actual size was: {}",
            VOLUME, processedGroups.size());
      }
    } catch (AccessDeniedException e) {
      // quietly swallow access denied
      LOG.debug(e.getLocalizedMessage(), e);
    } catch (SolrSearchException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new IllegalStateException(e);
    } catch (StorageClientException e) {
      throw new IllegalStateException(e);
    }
    long endTicks = System.currentTimeMillis();
    if (LOG.isDebugEnabled())
      LOG.debug("writeResults() took {} seconds",
          new Object[] { (float) (endTicks - startTicks) / 1000 });
  }

  /**
   * 
   * @param request
   * @param writer
   * @param auth
   * @param processedUsers
   * @throws AccessDeniedException
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void renderAuthorizable(final SlingHttpServletRequest request,
      final JSONWriter writer, final Authorizable auth, final Set<String> processedUsers)
      throws AccessDeniedException, JSONException, StorageClientException {

    final javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(
        javax.jcr.Session.class);
    try {
      if (auth != null && !processedUsers.contains(auth.getId())) {
        writer.object();
        final ValueMap map = profileService.getProfileMap(auth, jcrSession);
        ExtendedJSONWriter.writeValueMapInternals(writer, map);
        writer.endObject();

        processedUsers.add(auth.getId());
      }
    } catch (StorageClientException e) {
      LOG.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOG.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.sakaiproject.nakamura.api.search.solr.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {

    return searchServiceFactory.getSearchResultSet(request, query);
  }

}
