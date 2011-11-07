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
package org.sakaiproject.nakamura.site.search;

import static org.sakaiproject.nakamura.api.search.SearchUtil.escapeString;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.query.Query;

@Component(immediate = true, name = "ContentSearchPropertyProvider", label = "ContentSearchPropertyProvider", description = "Provides general properties for the content search")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides general properties for the content search"),
    @Property(name = "sakai.search.provider", value = "Content")
})
@Service(value = SearchPropertyProvider.class)
public class ContentSearchPropertyProvider implements SearchPropertyProvider {

  private static final String SITE_PARAM = "site";

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    RequestParameter siteParam = request.getRequestParameter(SITE_PARAM);
    if (siteParam != null) {
      String site = " AND @id = '" + escapeString(siteParam.getString(), Query.XPATH) + "'";
      propertiesMap.put("_site", site);
    }
  }
}
