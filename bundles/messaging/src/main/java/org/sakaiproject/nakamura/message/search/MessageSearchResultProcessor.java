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
package org.sakaiproject.nakamura.message.search;

import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_FROM;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_TO;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchResponseDecorator;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;

/**
 * Formats message node search results
 */
@Component
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Processor for message search results."),
    @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "Message")
})
public class MessageSearchResultProcessor implements SolrSearchResultProcessor, SearchResponseDecorator {

  enum ProfileType {
    TO, FROM
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageSearchResultProcessor.class);

  @Reference
  protected LiteMessagingService messagingService;

  @Reference
  protected SolrSearchServiceFactory searchServiceFactory;

  @Reference(referenceInterface = LiteMessageProfileWriter.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  protected Map<String, LiteMessageProfileWriter> writers = new ConcurrentHashMap<String, LiteMessageProfileWriter>();

  public void bindWriters(LiteMessageProfileWriter writer) {
    writers.put(writer.getType(), writer);
  }

  public void unbindWriters(LiteMessageProfileWriter writer) {
    writers.remove(writer.getType());
  }

  /**
   * Parses the message to a usable JSON format for the UI.
   *
   * @param write
   * @param result
   * @throws JSONException
   * @throws RepositoryException
   */
  public void writeResult(SlingHttpServletRequest request, JSONWriter write,
      Result result) throws JSONException {
    ResourceResolver resolver = request.getResourceResolver();
    // KERN-1573 no chat messages delivered
    // Content content = resolver.getResource(result.getPath()).adaptTo(Content.class);
    final Session session = StorageClientUtils.adaptToSession(resolver.adaptTo(javax.jcr.Session.class));
    try {
      final Content content = session.getContentManager().get(result.getPath());
      if (content == null) {
        // there is nothing to write
        return;
      }
      write.object();
      writeContent(request, write, content);
      write.endObject();
    } catch (StorageClientException e) {
      throw new JSONException(e);
    } catch (AccessDeniedException e) {
      throw new JSONException(e);
    }
  }

  public void writeContent(SlingHttpServletRequest request, JSONWriter write,
      Content content) throws AccessDeniedException, StorageClientException,
      JSONException {

    // Write out all the properties on the message.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, content);

    // Add some extra properties.
    write.key("id");
    String path = content.getPath();
    write.value(path.substring(path.lastIndexOf('/') + 1));

    javax.jcr.Session jcrSession =request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session = StorageClientUtils.adaptToSession(jcrSession);

    // Write out all the recipients their information on this message.
    // We always return this as an array, even if it is only 1 recipient.
    LiteMessageProfileWriter defaultProfileWriter = writers.get("internal");
    if (content.hasProperty(PROP_SAKAI_TO)) {
      String toVal = String.valueOf(content.getProperty(PROP_SAKAI_TO));
      String[] rcpts = StringUtils.split(toVal, ',');
      write.key("userTo");
      write.array();
      for (String rcpt : rcpts) {
        String[] values = StringUtils.split(rcpt, ':');
        LiteMessageProfileWriter writer = null;
        // usually it should be type:user. But in case the handler changed this..
        String user = values[0];
        if (values.length == 2) {
          user = values[1];
          String type = values[0];
          writer = writers.get(type);
        }
        if (writer == null) {
          writer = defaultProfileWriter;
        }
        write.object();
        writer.writeProfileInformation(session, user, write);
        decorateProfile(ProfileType.TO, session, user, write);
        write.endObject();
      }
      write.endArray();
    }

    // Although in most cases the sakai:from field will only contain 1 value.
    // We add in the option to support multiple cases.
    // For now we expect it to always be the user who sends the message.
    if (content.hasProperty(PROP_SAKAI_FROM)) {
      String fromVal = String.valueOf(content.getProperty(PROP_SAKAI_FROM));
      String[] senders = StringUtils.split(fromVal, ',');
      write.key("userFrom");
      write.array();
      for (String sender : senders) {
        write.object();
        defaultProfileWriter.writeProfileInformation(session, sender, write);
        decorateProfile(ProfileType.FROM, session, sender, write);
        write.endObject();
      }
      write.endArray();
    }

    // Write the previous message.
    if (content.hasProperty(PROP_SAKAI_PREVIOUS_MESSAGE)) {
      write.key("previousMessage");
      parsePreviousMessages(request, write, content);
    }
  }

  protected void decorateProfile(ProfileType profileType, Session session,
      String otherUser, JSONWriter write) throws AccessDeniedException,
      StorageClientException, JSONException {
    // default method has nothing to do. this is for subclassing
  }

  /**
   * Parse a message we have replied on.
   *
   * @param request
   * @param write
   * @param content
   * @throws JSONException
   */
  private void parsePreviousMessages(SlingHttpServletRequest request, JSONWriter write,
      Content content) throws JSONException {
    ResourceResolver resolver = request.getResourceResolver();
    javax.jcr.Session jcrSession = resolver.adaptTo(javax.jcr.Session.class);
    Session session = StorageClientUtils.adaptToSession(jcrSession);
    String userId = request.getRemoteUser();
    String id = (String) content
        .getProperty(PROP_SAKAI_PREVIOUS_MESSAGE);
    try {
      Content previousMessage = searchMailboxes(userId, session, id);
      if ( previousMessage != null ) {
        write.object();
        writeContent(request, write, previousMessage);
        write.endObject();
      } else {
        write.value(false);
      }
    } catch (StorageClientException e) {
      throw new JSONException("Couldn't write search results because couldn't get message with id " + id);
    } catch (AccessDeniedException e) {
      throw new JSONException("Couldn't write search results because did not have permission to get message with id " + id);
    }
  }

  private Content searchMailboxes(String userId, Session session, String id) throws StorageClientException, AccessDeniedException {
    ContentManager contentManager = session.getContentManager();
    String messageStore = messagingService.getFullPathToStore(userId, session);
    String path = messageStore + MessageConstants.BOX_OUTBOX + "/" + id;
    if (!contentManager.exists(path)) {
      path = messageStore + MessageConstants.BOX_INBOX + "/" + id;
    }
    return contentManager.get(path);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      java.lang.String)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  public void decorateSearchResponse(SlingHttpServletRequest request, JSONWriter writer)
    throws JSONException {
    writer.key("unread");

    long count = 0;
    // We don't do queries for anonymous users. (Possible ddos hole).
    String userID = request.getRemoteUser();
    if (UserConstants.ANON_USERID.equals(userID)) {
      writer.value(count);
      return;
    }

    try {
      final Session session = StorageClientUtils.adaptToSession(request
          .getResourceResolver().adaptTo(javax.jcr.Session.class));
      String store = messagingService.getFullPathToStore(userID, session);
      store = ISO9075.encodePath(store);
      store = store.substring(0, store.length() - 1);
      String queryString = "path:" + ClientUtils.escapeQueryChars(store) +
        " AND resourceType:sakai/message AND type:internal AND messagebox:inbox AND read:false";
      Query query = new Query(queryString);
      SolrSearchResultSet resultSet = searchServiceFactory.getSearchResultSet(
          request, query, false);
      count = resultSet.getSize();
    } catch (SolrSearchException e) {
      LOGGER.error(e.getMessage());
    } finally {
      writer.value(count);
    }
  }
}
