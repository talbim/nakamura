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
package org.sakaiproject.nakamura.user.counts;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ContentCounter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentCounter.class);

  public int countExact(Authorizable au, SolrServerService solrSearchService) {
    if ( au != null && !CountProvider.IGNORE_AUTHIDS.contains(au.getId())) {
      // find the content where the user has been made either a viewer or a manager
      String userID = ClientUtils.escapeQueryChars(au.getId());
      String readers = userID;
      if (!au.isGroup()) {
        // pooled-content-manager, pooled-content-viewer
        List<String> principals = Lists.newArrayList(userID);
        if (au.getPrincipals() != null) {
          for (String principal : au.getPrincipals()) {
            principals.add(ClientUtils.escapeQueryChars(principal));
          }
        }
        principals.remove(Group.EVERYONE);
        readers = StringUtils.join(principals, " OR ");
      }

      String queryString = "resourceType:sakai/pooled-content AND (manager:(" + readers
          + ") OR viewer:(" + readers + "))";
      return getCount(queryString, solrSearchService); 
    }
    return 0;
  }
  
  /**
   * @param queryString
   * @param solrSearchService 
   * @return the count of results, we assume if they are returned the user can read them
   *         and we do not iterate through the entire set to check.
   */
  private int getCount(String queryString, SolrServerService solrSearchService) {
    SolrServer solrServer = solrSearchService.getServer();
    SolrQuery solrQuery = new SolrQuery(queryString);

    QueryResponse response;
    try {
      response = solrServer.query(solrQuery);
      return (int) response.getResults().getNumFound();
    } catch (SolrServerException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return 0;
  }


}
