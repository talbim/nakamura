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
package org.sakaiproject.nakamura.activity;

import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_STORE_NAME;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.LITE_EVENT_TOPIC;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_ACTOR_ID;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

@Component(immediate=true, metatype=true)
@Service(value=ActivityService.class)
public class ActivityServiceImpl implements ActivityService {

  @Reference
  private EventAdmin eventAdmin;

  public void createActivity(Session session, Content targetLocation,  String userId, ActivityServiceCallback callback) throws AccessDeniedException, StorageClientException, ServletException, IOException {
    if ( userId == null ) {
      userId = session.getUserId();
    }
    if ( !userId.equals(session.getUserId()) && !User.ADMIN_USER.equals(session.getUserId()) ) {
      throw new IllegalStateException("Only Administrative sessions may act on behalf of another user for activities");
    }
    ContentManager contentManager = session.getContentManager();
    // create activityStore if it does not exist
    String path = StorageClientUtils.newPath(targetLocation.getPath(), ACTIVITY_STORE_NAME);
    if (!contentManager.exists(path)) {
      contentManager.update(new Content(path, ImmutableMap.of(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          (Object) ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE)));
      // set ACLs so that everyone can add activities; anonymous = none.
      session.getAccessControlManager().setAcl(
          Security.ZONE_CONTENT,
          path,
          new AclModification[] {
              new AclModification(AclModification.denyKey(User.ANON_USER),
                  Permissions.ALL.getPermission(), Operation.OP_REPLACE),
              new AclModification(AclModification.grantKey(Group.EVERYONE),
                  Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE),
              new AclModification(AclModification.grantKey(Group.EVERYONE),
                  Permissions.CAN_WRITE.getPermission(), Operation.OP_REPLACE),
              new AclModification(AclModification.grantKey(userId),
                  Permissions.ALL.getPermission(), Operation.OP_REPLACE) });
    }
    // create activity within activityStore
    String activityPath = StorageClientUtils.newPath(path, ActivityUtils.createId());
    String activityFeedPath = StorageClientUtils.newPath(targetLocation.getPath(), "activityFeed");

    if (!contentManager.exists(activityFeedPath)) {
      contentManager.update(new Content(activityFeedPath, null));
    }
    if (!contentManager.exists(activityPath)) {
      contentManager.update(new Content(activityPath, ImmutableMap.of(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          (Object) ActivityConstants.ACTIVITY_SOURCE_ITEM_RESOURCE_TYPE)));
    }

    
    Content activtyNode = contentManager.get(activityPath);
    callback.processRequest(activtyNode);


    activtyNode = contentManager.get(activityPath);
    activtyNode.setProperty(PARAM_ACTOR_ID, userId);
    activtyNode.setProperty(ActivityConstants.PARAM_SOURCE, targetLocation.getPath());
    contentManager.update(activtyNode);
    // post the asynchronous OSGi event
    final Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(UserConstants.EVENT_PROP_USERID, userId);
    properties.put(ActivityConstants.EVENT_PROP_PATH, activityPath);
    properties.put("path", activityPath);
    properties.put("resourceType", ActivityConstants.ACTIVITY_SOURCE_ITEM_RESOURCE_TYPE);
    EventUtils.sendOsgiEvent(properties, LITE_EVENT_TOPIC, eventAdmin);
  }

}
