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
package org.sakaiproject.nakamura.basiclti;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.basiclti.LiteBasicLTIContextIdResolver;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * The default mapping strategy will be to simply return the {@link Content#getPath()} as
 * the LTI context_id. For example: <code>/sites/12345</code>.
 * 
 * If the {@link Content} has a special property, <code>lti_context_id</code>, then use
 * the value of that property as the context_id. See: {@link #LTI_CONTEXT_ID}.
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class LiteDefaultContextIdResolver implements LiteBasicLTIContextIdResolver {
  private static final Logger LOG = LoggerFactory
      .getLogger(LiteDefaultContextIdResolver.class);

  /**
   * The {@link Content} property key used to store an LTI context_id.
   */
  @Property(value = "lti_context_id", name = "LTI_CONTEXT_ID", label = "key", description = "The Content property key used to store an LTI context_id.")
  public static final String LTI_CONTEXT_ID = LiteDefaultContextIdResolver.class
      .getName() + ".key";
  private String key = "lti_context_id";

  /**
   * {@inheritDoc}
   * 
   * @see {@link LiteBasicLTIContextIdResolver#resolveContextId(Content)}
   */
  public String resolveContextId(final Content node) {
    LOG.debug("resolveContextId(Content {})", node);
    if (node == null) {
      throw new IllegalArgumentException("Content cannot be null");
    }

    String contextId = null;
    if (node.hasProperty(key)) {
      // we have a special context_id we can use
      contextId = (String) node.getProperty(key);
    } else {
      // just use the path
      contextId = node.getPath();
    }
    return contextId;
  }

  protected void activate(ComponentContext context) {
    @SuppressWarnings("rawtypes")
    final Dictionary props = context.getProperties();
    key = OsgiUtil.toString(props.get(LTI_CONTEXT_ID), "lti_context_id");
  }
}
