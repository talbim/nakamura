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
package org.sakaiproject.nakamura.rootconfig.impl;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(metatype = true, immediate = true)
public class RootConfigurationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RootConfigurationService.class);

  @Property(value="", description="The path to which the root URL should redirect. Sling default is '/index.html'. Empty string means no change.")
  static final String ROOT_PATH = "root.path";

  /** Not exposed yet by a public Sling API */
  static final String PROP_SLING_REDIRECT_TARGET = "sling:target";
  static final String PROP_SLING_REDIRECT_TYPE = "sling:redirect";

  protected String rootPath;

  @Reference
  protected transient SlingRepository repository;

  //----------- OSGi integration ----------------------------

  @Activate
  protected void activate(Map<?, ?> properties) {
    init(properties);
  }

  @Modified
  protected void modified(Map<?, ?> properties) {
    init(properties);
  }

  //----------- Internal ----------------------------

  private void init(Map<?, ?> properties) {
    rootPath = OsgiUtil.toString(properties.get(ROOT_PATH), "");
    if (rootPath.length() > 0) {
      setRootPath(rootPath);
    }
  }

  private void setRootPath(String path) {
    Session session = null;
    try {
      session = repository.loginAdministrative(null);
      Node rootNode = session.getNode("/");
      String resourceType = null;
      if (rootNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
        resourceType = rootNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
      }
      if (!PROP_SLING_REDIRECT_TYPE.equals(resourceType)) {
        LOGGER.warn("Asked to set a non-redirecting root node of type {}", resourceType);
        rootNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, PROP_SLING_REDIRECT_TYPE);
      }
      String oldRootPath = null;
      if (rootNode.hasProperty(PROP_SLING_REDIRECT_TARGET)) {
        javax.jcr.Property redirectTarget = rootNode.getProperty(PROP_SLING_REDIRECT_TARGET);
        oldRootPath = redirectTarget.getString();
      }
      if (!path.equals(oldRootPath)) {
        LOGGER.info("Changing root path from {} to {}", oldRootPath, path);
        rootNode.setProperty(PROP_SLING_REDIRECT_TARGET, path);
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOGGER.error("Error setting root path to " + path, e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }
}
