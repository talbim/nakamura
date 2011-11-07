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
package org.sakaiproject.nakamura.auth.ldap;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessService;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationPluginTest {

  private final static String LDAP_USER = "uid=admin,ou=Special People,o=nyu.edu,o=nyu";
  private final static String LDAP_PASS = "admin";
  private final static String LDAP_BASE_DN = "ou=People,o=nyu.edu,o=nyu";
  private final static String USER_FILTER = "uid={}";
  private final static String AUTHZ_FILTER = "eduEntitlements=sakai";
  private final static String USER = "joe";
  private final static String PASS = "pass";

  private LdapAuthenticationPlugin ldapAuthenticationPlugin;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private LdapConnectionManager connMgr;

  @Mock
  private LDAPConnection conn;

  @Mock
  private LDAPSearchResults results;

  @Mock
  private LiteAuthorizablePostProcessService LiteAuthorizablePostProcessService;

  @Mock
  private Repository repository;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Session session;

  @Mock
  private AuthorizableManager authorizableManager;

  @Before
  public void setup() throws Exception {
    when(connMgr.getConfig().getLdapUser()).thenReturn(LDAP_USER);
    when(connMgr.getConfig().getLdapPassword()).thenReturn(LDAP_PASS);

    when(repository.loginAdministrative()).thenReturn(session);

    when(session.getAuthorizableManager()).thenReturn(authorizableManager);

    ldapAuthenticationPlugin = new LdapAuthenticationPlugin(connMgr,
        LiteAuthorizablePostProcessService, repository);
  }

  @Test
  public void defaultConstructor() {
    new LdapAuthenticationPlugin();
  }

  @Test
  public void activateModify() {
    new LdapAuthenticationPlugin().activate(new HashMap<String, String>());
    new LdapAuthenticationPlugin().modified(new HashMap<String, String>());
  }

  @Test
  public void authenticationFailsIfNotSimpleCredentials() throws Exception {
    // given
    Credentials credentials = mock(Credentials.class);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(credentials));
  }

  @Test
  public void authenticateWithValidCredentialsNoFilter() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue()).thenReturn(
        "inetOrgPerson");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertFalse(ldapAuthenticationPlugin.canDecorateUser());

    verify(ldapEntry).getDN();
  }

  @Test
  public void authenticateWithValidCredentialsWithFilter() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), eq(LDAPConnection.SCOPE_SUB), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue()).thenReturn(
        "inetOrgPerson");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertFalse(ldapAuthenticationPlugin.canDecorateUser());

    verify(ldapEntry).getDN();
  }

  @Test
  public void useAliasObject() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class), any(String[].class),
            anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());

    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue())
        .thenReturn("aliasObject");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertFalse(ldapAuthenticationPlugin.canDecorateUser());

    // verify that the alias attributes where accessed
    verify(ldapEntry, atLeastOnce()).getAttribute("objectClass");
    verify(ldapEntry, atLeastOnce()).getAttribute("aliasedObjectName");
  }

  @Test
  public void failConnectingToLdapHost() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenThrow(new LDAPException());

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @Test
  public void failBindingAsAppUser() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    doThrow(new LDAPException()).when(conn).bind(LDAPConnection.LDAP_V3, LDAP_USER,
        LDAP_PASS.getBytes("UTF-8"));

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @Test
  public void failSearchingForUser() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(false);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @Test
  public void failBindingAsUser() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    doThrow(new LDAPException()).when(conn).bind(LDAPConnection.LDAP_V3, userEntryDn,
        PASS.getBytes("UTF-8"));

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @Test
  public void failBindingAsAppUserAfterUserSearch() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    doNothing().doThrow(new LDAPException()).when(conn)
        .bind(LDAPConnection.LDAP_V3, LDAP_USER, LDAP_PASS.getBytes("UTF-8"));
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @Test
  public void failAuthzFilterSearch() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true).thenReturn(false);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createAccountAfterAuth() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.TRUE);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(attr.getStringValue()).thenReturn("standard");
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    when(authorizableManager.createUser(isA(String.class), isA(String.class), isA(String.class), (Map<String, Object>) isNull())).thenReturn(true);
    
    User user = mock(User.class);
    when(user.getId()).thenReturn(USER);
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(null);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS.toCharArray())));

    verify(authorizableManager).createUser(isA(String.class), isA(String.class), isA(String.class), (Map<String, Object>) isNull());
  }

  @Test
  public void decorateUserAfterAuthUsingStringArray() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    props.put(LdapAuthenticationPlugin.USER_PROPS, new String[] {
        "\"givenName\":\"firstName\"", "\"sn\":\"lastName\"", "\"mail\":\"email\"" });
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(attr.getStringValue()).thenReturn("standard");
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    Authorizable user = mock(Authorizable.class);
    when(user.getId()).thenReturn("testuser");
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(user);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertTrue(ldapAuthenticationPlugin.canDecorateUser());

    verify(entry).getAttribute("givenName");
    verify(user).setProperty(eq("firstName"), isA(String.class));

    verify(entry).getAttribute("sn");
    verify(user).setProperty(eq("lastName"), isA(String.class));

    verify(entry).getAttribute("mail");
    verify(user).setProperty(eq("email"), isA(String.class));
  }

  @Test
  public void decorateUserAfterAuthWithJsonString() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    props.put(LdapAuthenticationPlugin.USER_PROPS,
        "{\"givenName\":\"firstName\", \"sn\":\"lastName\", \"mail\":\"email\"}");
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class), any(String[].class),
            anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(attr.getStringValue()).thenReturn("standard");
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    Authorizable user = mock(User.class);
    when(user.getId()).thenReturn("testuser");
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(user);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertTrue(ldapAuthenticationPlugin.canDecorateUser());

    verify(entry).getAttribute("givenName");
    verify(user).setProperty(eq("firstName"), isA(String.class));

    verify(entry).getAttribute("sn");
    verify(user).setProperty(eq("lastName"), isA(String.class));

    verify(entry).getAttribute("mail");
    verify(user).setProperty(eq("email"), isA(String.class));
  }

  @Test
  public void decorateUserAfterAuthWithKeyedValues() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    props.put(LdapAuthenticationPlugin.USER_PROPS + "givenName", "firstName");
    props.put(LdapAuthenticationPlugin.USER_PROPS + "sn", "lastName");
    props.put(LdapAuthenticationPlugin.USER_PROPS + "mail", "email");
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(attr.getStringValue()).thenReturn("standard");
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    User user = mock(User.class);
    when(user.getId()).thenReturn("testuser");
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(user);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));
    assertTrue(ldapAuthenticationPlugin.canDecorateUser());

    verify(entry).getAttribute("givenName");
    verify(user).setProperty(eq("firstName"), isA(String.class));

    verify(entry).getAttribute("sn");
    verify(user).setProperty(eq("lastName"), isA(String.class));

    verify(entry).getAttribute("mail");
    verify(user).setProperty(eq("email"), isA(String.class));
  }

  @Test
  public void clearAttrsPropsWithNullProps() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    props.put(LdapAuthenticationPlugin.USER_PROPS, null);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class), any(String[].class),
            anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    User user = mock(User.class);
    when(user.getId()).thenReturn("testuser");
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(user);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));

    assertFalse(ldapAuthenticationPlugin.canDecorateUser());
  }

  @Test
  public void clearAttrsPropsWithNullArrayEntry() throws Exception {
    // given
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.CREATE_ACCOUNT, Boolean.FALSE);
    props.put(LdapAuthenticationPlugin.USER_PROPS, new String[] { null });
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(isA(String.class), anyInt(), isA(String.class), any(String[].class),
            anyBoolean())).thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);

    LDAPEntry entry = mock(LDAPEntry.class);
    when(results.next()).thenReturn(entry);

    String userEntryDn = USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN;
    when(entry.getDN()).thenReturn(userEntryDn);

    LDAPAttribute attr = mock(LDAPAttribute.class);
    when(entry.getAttribute(isA(String.class))).thenReturn(attr);

    User user = mock(User.class);
    when(user.getId()).thenReturn("testuser");
    when(authorizableManager.findAuthorizable(isA(String.class))).thenReturn(user);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(new SimpleCredentials(USER, PASS
        .toCharArray())));

    assertFalse(ldapAuthenticationPlugin.canDecorateUser());
  }
}
