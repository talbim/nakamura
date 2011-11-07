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
package org.sakaiproject.nakamura.rules.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionErrorListener;
import org.sakaiproject.nakamura.api.rules.RuleExecutionException;
import org.sakaiproject.nakamura.api.rules.RuleExecutionService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

public class ExecuteRuleServletTest {

  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private RuleExecutionService ruleExecutionService;

  public ExecuteRuleServletTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDoGetNoRuleSet() throws ServletException, IOException, JSONException {

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    Mockito.when(response.getWriter()).thenReturn(printWriter);

    ArgumentCaptor<Integer> errorCode = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass(String.class);

    ExecuteRuleServlet ruleServlet = new ExecuteRuleServlet();
    ruleServlet.ruleExecutionService = ruleExecutionService;
    ruleServlet.doGet(request, response);

    Mockito.verify(response).sendError(errorCode.capture(), errorMessage.capture());

    Assert.assertEquals(400, errorCode.getValue().intValue());

  }

  @Test
  public void testDoGetNoRuleByPath() throws ServletException, IOException, JSONException, RuleExecutionException {
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    Mockito.when(response.getWriter()).thenReturn(printWriter);
    
    Mockito.when(request.getParameter(ExecuteRuleServlet.PARAM_RULE_SET)).thenReturn("/rulesetOne");
    Map<String, Object> resultMap = new HashMap<String, Object>();
    resultMap.put("result", "ok");
    Mockito.when(ruleExecutionService.executeRuleSet(Mockito.eq("/rulesetOne"), 
        Mockito.eq(request), 
        Mockito.any(Resource.class), 
        Mockito.any(RuleContext.class), 
        Mockito.any(RuleExecutionErrorListener.class))).thenReturn(resultMap);
    
    
    ExecuteRuleServlet ruleServlet = new ExecuteRuleServlet();
    ruleServlet.ruleExecutionService = ruleExecutionService;
    ruleServlet.doGet(request, response);
     
      
    JSONObject result = new JSONObject(stringWriter.toString());
    Assert.assertEquals("ok",result.get("result"));
    
    
  }
}
