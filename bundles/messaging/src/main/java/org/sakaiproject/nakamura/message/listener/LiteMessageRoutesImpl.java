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
package org.sakaiproject.nakamura.message.listener;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;

import java.util.ArrayList;


/**
 *
 */
public class LiteMessageRoutesImpl extends ArrayList<MessageRoute> implements MessageRoutes {

  /**
   *
   */
  private static final long serialVersionUID = 5838090931972965691L;

  /**
   * @param message
   */
  public LiteMessageRoutesImpl(Content message) {
    String toProp = (String) message.getProperty(MessageConstants.PROP_SAKAI_TO);
    String[] recipients = StringUtils.split(toProp, ",");
    for (String r : recipients) {
      add(new MessageRouteImpl(r));
    }
  }
}
