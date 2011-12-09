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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;

import java.util.List;

import javax.jcr.RepositoryException;

@ServiceDocumentation(name = "RemovePropertyOperation documentation", okForVersion = "1.1",
  shortDescription = "Allows removing a property or properties from one or more resources.",
  description = "Allows removing a property or properties from one or more resources.",
  methods = {
    @ServiceMethod(name = "POST", description = "The Remove Property operation is only invoked with a POST.",
      parameters = {
        @ServiceParameter(name = ":operation", description = "This must be 'removeProperty' for this operation to be invoked."),
        @ServiceParameter(name = ":applyTo", description = "Stores the path or paths of resources to add properties to. If this is left out, then the request path is used.")
      })
})
@Component(immediate = true)
@Service
public class RemovePropertyOperation extends AbstractPropertyOperationModifier {

  @Property(value = "removeProperty")
  static final String SLING_POST_OPERATION = "sling.post.operation";

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    doModify(request, response, changes);

  }

}
