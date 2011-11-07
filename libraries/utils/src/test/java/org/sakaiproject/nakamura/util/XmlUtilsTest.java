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
package org.sakaiproject.nakamura.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlUtilsTest {

  @Test
  public void testEncode() {
    String xmlString = "Lorem<>&\"\t\n\ripsum";
    assertEquals(XmlUtils.encode(xmlString),
        "Lorem&lt;&gt;&amp;&quot;\t\n\ripsum");
    assertEquals(XmlUtils.encode("<"), "&lt;");
    assertEquals(XmlUtils.encode(">"), "&gt;");
    assertEquals(XmlUtils.encode("&"), "&amp;");
    assertEquals(XmlUtils.encode("\""), "&quot;");
    assertEquals(XmlUtils.encode("\t"), "\t");
    assertEquals(XmlUtils.encode("\n"), "\n");
    assertEquals(XmlUtils.encode("\r"), "\r");
    assertEquals(XmlUtils.encode("Lorem"), "Lorem");
    assertEquals(XmlUtils.encode(""), "");
    assertEquals(XmlUtils.encode(null), "");
  }
}
