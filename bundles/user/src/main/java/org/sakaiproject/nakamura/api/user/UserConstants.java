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
package org.sakaiproject.nakamura.api.user;

/**
 *
 */
public interface UserConstants {
  public static final String SYSTEM_USER_MANAGER_PATH = "/system/userManager";

  public static final String SYSTEM_USER_MANAGER_USER_PATH = SYSTEM_USER_MANAGER_PATH
      + "/user";
  public static final String SYSTEM_USER_MANAGER_GROUP_PATH = SYSTEM_USER_MANAGER_PATH
      + "/group";

  public static final String SYSTEM_USER_MANAGER_USER_PREFIX = SYSTEM_USER_MANAGER_USER_PATH
      + "/";
  public static final String SYSTEM_USER_MANAGER_GROUP_PREFIX = SYSTEM_USER_MANAGER_GROUP_PATH
      + "/";

  public static final String USER_PROFILE_RESOURCE_TYPE = "sakai/user-profile";
  public static final String GROUP_PROFILE_RESOURCE_TYPE = "sakai/group-profile";
  public static final String USER_HOME_RESOURCE_TYPE = "sakai/user-home";
  public static final String GROUP_HOME_RESOURCE_TYPE = "sakai/group-home";

  /**
   * A list of private properties that will not be copied from the authorizable.
   */
  public static final String PRIVATE_PROPERTIES = "sakai:privateproperties";

  /**
   * The number of hash levels applied to user paths, this is system wide and can't be
   * changed once an instance has been loaded with users. 4 will give upto 2E9 users.
   */
  public static final int DEFAULT_HASH_LEVELS = 4;


  /**
   * A node property that indicates which use the node was created by, for ownership.
   */
  public static final String JCR_CREATED_BY = "jcr:createdBy";


  /**
   * The ID of an anon user.
   */
  public static final String ANON_USERID = "anonymous";

  public static final String ADMIN_USERID = "admin";

  /**
   * An array of managers of a group
   */
  public static final String PROP_GROUP_MANAGERS = "rep:group-managers";

  /**
   * An array of viewers of a group
   */
  public static final String PROP_GROUP_VIEWERS = "rep:group-viewers";

  /**
   * The ID of the special group that manages a group.
   */
  public static final String PROP_MANAGERS_GROUP = "sakai:managers-group";
  public static final String PROP_MANAGED_GROUP = "sakai:managed-group";
  public static final String PROP_JOINABLE_GROUP = "sakai:group-joinable";
  public static final String PROP_PSEUDO_GROUP = "sakai:pseudoGroup";
  public static final String PROP_PSEUDO_GROUP_PARENT = "sakai:pseudogroupparent";
  
  /**
   * Bare Authorizables have no /~ content and don't do any post processing.
   */
  public static final String PROP_BARE_AUTHORIZABLE = "sakai:bare";

  /**
   * The name of the property that holds the value of identifier (authorizable ID) for
   * this profile.
   */
  public static final String USER_IDENTIFIER_PROPERTY = "rep:userId";

  /**
   * The property name that holds the given name of a user.
   */
  public static final String USER_FIRSTNAME_PROPERTY = "firstName";

  /**
   * The property name that holds the family name of a user.
   */
  public static final String USER_LASTNAME_PROPERTY = "lastName";

  /**
   * The property name that holds the email of a user.
   */
  public static final String USER_EMAIL_PROPERTY = "email";
  
  /**
   * The property name that holds the picture location for a user.
   */
  public static final String USER_PICTURE = "picture";
  
  /**
   * The name of the property which holds the user's institutional role (faculty, staff, etc.)
   */
  public static final String USER_ROLE = "role";

  /**
   * The name of the property which holds the user's department (Chemistry, English, etc.)
   */
  public static final String USER_DEPARTMENT = "department";

  /**
   * The name of the property which holds the user's college (Liberal Arts, Natural Sciences, etc.)
   */
  public static final String USER_COLLEGE = "college";

  /**
   * The name of the property which holds the user's date of birth.
   */
  public static final String USER_DATEOFBIRTH = "dateofbirth";
  
  /**
   * The name of the property which holds the list of tag names this user is tagged with.
   */
  public static final String USER_TAGS = "sakai:tags";

  /**
   * The name of the property which holds the name that this user prefers to go by.
   */
  public static final String PREFERRED_NAME = "preferredName";
  
  /**
   * The property name that holds the basic nodes for a user.
   */
  public static final String USER_BASIC = "basic";
  
  /**
   * The property name that holds the access information for a user.
   */
  public static final String USER_BASIC_ACCESS = "access";

  /**
   * Default value for the access property.
   */
  public static final String EVERYBODY_ACCESS_VALUE = "everybody";
  
  /**
   * Property name for the full title/name of a group. ie: Title: The 2010 Mathematics 101
   * class. Authorizable id: the-2010-mathematics-101-class
   */
  public static final String GROUP_TITLE_PROPERTY = "sakai:group-title";

  /**
   * Property name for the description of a group.
   */
  public static final String GROUP_DESCRIPTION_PROPERTY = "sakai:group-description";

  /**
   * The joinable property
   */
  public enum Joinable {
    /**
     * The group is joinable.
     */
    yes(),
    /**
     * The group is not joinable.
     */
    no(),
    /**
     * The group is joinable with approval.
     */
    withauth();
  }

  /**
   * The Authorizable node's subpath within the repository's user or group tree.
   */
  public static final String PROP_AUTHORIZABLE_PATH = "path";

  public static final String USER_REPO_LOCATION = "/rep:security/rep:authorizables/rep:users";
  public static final String GROUP_REPO_LOCATION = "/rep:security/rep:authorizables/rep:groups";


  /**
   * The key name for the property in the event that will hold the userid.
   */
  public static final String EVENT_PROP_USERID = "userid";

  /**
   * The name of the OSGi event topic for creating a user.
   */
  public static final String TOPIC_USER_CREATED = "org/sakaiproject/nakamura/lite/user/created";

  /**
   * The name of the OSGi event topic for updating a user.
   */
  public static final String TOPIC_USER_UPDATE = "org/sakaiproject/nakamura/lite/user/updated";

  /**
   * The name of the OSGi event topic for deleting a user.
   */
  public static final String TOPIC_USER_DELETED = "org/sakaiproject/nakamura/lite/user/deleted";

  /**
   * The name of the OSGi event topic for creating a group.
   */
  public static final String TOPIC_GROUP_CREATED = "org/sakaiproject/nakamura/lite/group/created";

  /**
   * The name of the OSGi event topic for updating a group.
   */
  public static final String TOPIC_GROUP_UPDATE = "org/sakaiproject/nakamura/lite/group/updated";

  /**
   * The name of the OSGi event topic for deleting a group.
   */
  public static final String TOPIC_GROUP_DELETED = "org/sakaiproject/nakamura/lite/group/deleted";
  
  
  /**
   * Property name for the parent of all counts in the profile.
   */
  public static final String COUNTS_PROP = "counts";
  /**
   * Property name for the number of contacts the user has.
   */
  public static final String CONTACTS_PROP = "contactsCount";
  /**
   * Property name for the number of groups that an authourizable is a member of.
   */
  public static final String GROUP_MEMBERSHIPS_PROP = "membershipsCount";  // the number of groups a user belongs to
  /**
   * Property name for the number of content items that the authorizable is listed as a manager or viewer.
   */
  public static final String CONTENT_ITEMS_PROP = "contentCount";
  /**
   * The epoch when the counts were last updated. 
   */
  public static final String COUNTS_LAST_UPDATE_PROP = "countLastUpdate";
  /**
   * Property name for the number of members that a group has (int)
   */
  public static final String GROUP_MEMBERS_PROP = "membersCount"; // the number of members that a group has

  /**
   * If present and true, the authorizable will not appear in the search index.
   */
  public static final String SAKAI_EXCLUDE = "sakai:excludeSearch";


  public static final String SAKAI_CATEGORY = "sakai:category";

}
