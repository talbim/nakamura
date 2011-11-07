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
package org.sakaiproject.nakamura.messagebucket;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucket;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketService;
import org.sakaiproject.nakamura.api.messagebucket.Waiter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(metatype=true, immediate=true)
public class UIEventServlet extends HttpServlet {

  
  
  /**
   * 
   */
  private static final long serialVersionUID = 5305310297450357095L;

  @Property(longValue=120000L)
  private static final String TIMEOUT_CONFIG = "polltimeout";
  
  @Reference
  private HttpService httpService;

  @Reference
  private MessageBucketService bucketService;

  private long timeout;

  @Activate
  public void activate(Map<String, Object> properties) throws ServletException, NamespaceException {
    timeout = OsgiUtil.toLong(properties.get(TIMEOUT_CONFIG), 120000L);
     httpService.registerServlet("/system/uievent/default", this, null, null);
  }
  
  @Deactivate
  public void deactivate(Map<String, Object> properties) {
    httpService.unregister("/system/uievent/default");
  }

  
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String token = request.getParameter("token");
    try {
      MessageBucket mb =  bucketService.getBucket(token);
       mb.bind(token, request);
      try {
      synchronized (mb) {
        if ( mb.isReady() ) {
          mb.send(response);
        } else {
          Continuation continuation = ContinuationSupport.getContinuation(request, mb);
          Waiter waiter = new ContinuationWaiter(continuation);
          mb.addWaiter(waiter);
          continuation.suspend(timeout);
          mb.removeWaiter(waiter);
          if ( mb.isReady() ) {
            mb.send(response);
          } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,"Timed out waiting for message bucket to fill");
          }
        }      
      }
      } finally {
        mb.unbind(token, request);
      }
    } catch ( MessageBucketException e ) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,e.getMessage());
    }
  }

  
}
