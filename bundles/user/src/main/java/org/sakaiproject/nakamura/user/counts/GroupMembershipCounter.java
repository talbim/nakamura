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

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Membership counter.
 */
public class GroupMembershipCounter {
  
  private static final int MAX_GROUP_COUNT = 5000; // to limit iteration through groups count to prevent any DOS attacks there
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupMembershipCounter.class);


  /**
   * Counts the Groups this authorizable is a member of excluding everyone. Includes group that the member is indirectly a memberOf
   * @param au the authorizable
   * @param authorizableManager
   * @return the number of unique groups the authorizable is a member of, including idirect and intermediage membership.
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  public int count(Authorizable au, AuthorizableManager authorizableManager) throws AccessDeniedException, StorageClientException {
    
    int count = 0;
    if ( au != null && !CountProvider.IGNORE_AUTHIDS.contains(au.getId())) {
      // code borrowed from LiteMeServlet to include indirect memberships
      // KERN-1831 changed from getPrincipals to memberOf to drill down list
      for (Iterator<Group> memberOf = au.memberOf(authorizableManager); memberOf.hasNext();) {
        Authorizable group = memberOf.next();
        if (group == null || !(group instanceof Group)
            // we don't want to count the everyone groups
            || CountProvider.IGNORE_AUTHIDS.contains(group.getId())
            // don't count if the group is to be excluded
            || Boolean.parseBoolean(String.valueOf(group.getProperty("sakai:excludeSearch")))
            // don't count if the group lacks a title
            || group.getProperty("sakai:group-title") == null
            || StringUtils.isEmpty(String.valueOf(group.getProperty("sakai:group-title")))
            // don't count the special "contacts" group
            || group.getId().startsWith("g-contacts-")) {
          continue;
        }
        if (group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
          // fetch the group that the manager group manages
          Authorizable managedGroup = authorizableManager.findAuthorizable((String) group
              .getProperty(UserConstants.PROP_MANAGED_GROUP));
          if (managedGroup == null || !(managedGroup instanceof Group)) {
            // dont count this group if the managed group doesnt exist. (ieb why ?, the users is still a member of this group even if the managed group doesnt exist)
            continue;
          }
        }
        count++;
        if (count >= MAX_GROUP_COUNT) {
          LOGGER.warn("getGroupsCount() has reached its maximum of {} check for reason, possible DOS issue?", new Object[]{MAX_GROUP_COUNT});
          return count;
        }
      }
    }
    return count;
  }

}
