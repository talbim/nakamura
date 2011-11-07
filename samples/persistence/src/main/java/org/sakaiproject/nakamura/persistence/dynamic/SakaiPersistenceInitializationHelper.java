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
package org.sakaiproject.nakamura.persistence.dynamic;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceInitializationHelper;

import java.util.Map;

public class SakaiPersistenceInitializationHelper extends PersistenceInitializationHelper {

  private ClassLoader amalgamatedClassloader;

  public SakaiPersistenceInitializationHelper(ClassLoader amalgamatedClassloader) {
    this.amalgamatedClassloader = amalgamatedClassloader;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public ClassLoader getClassLoader(String emName, Map properties) {
    if (properties != null) {
      return (ClassLoader) properties.get(PersistenceUnitProperties.CLASSLOADER);
    } else {
      return amalgamatedClassloader;
    }
  }

}
