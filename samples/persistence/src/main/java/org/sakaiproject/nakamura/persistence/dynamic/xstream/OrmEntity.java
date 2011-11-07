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
package org.sakaiproject.nakamura.persistence.dynamic.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("entity")
public class OrmEntity {

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof OrmEntity)) {
      return false;
    }
    OrmEntity other = (OrmEntity) obj;
    return other.getClassName().equals(getClassName());
  }

  @Override
  public int hashCode() {
    return getClassName().hashCode();
  }

  private String className;

  public void setClassName(String className) {
    this.className = className;
  }

  public String getClassName() {
    return (className != null ? className : "");
  }
}
