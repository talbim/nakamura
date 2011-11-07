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
package org.sakaiproject.nakamura.api.search.solr;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Query container for searching. The query string and options are stored separately for
 * greater ease when parsing as templates and for building the SolrQuery object.
 */
public class Query {
  public static final String SPARSE = "sparse";
  public static final String SOLR = "solr";

  private String type;

  private String queryString;

  private Map<String, String> options;

  private String name;

  public Query(String queryString) {
    if (StringUtils.isBlank(queryString)) {
      throw new IllegalArgumentException("'queryString' must be provided to query");
    }

    this.type = Query.SOLR;
    this.queryString = queryString;
    this.name = null;
  }

  /**
   * Create a query with a query string and optional properties to use Solr for searching.
   * 
   * @param queryString
   * @param options
   */
  public Query(String queryString, Map<String, String> options) {
    this(queryString);

    this.options = options;
    this.name = null;
  }

  /**
   * Create a query with properties to be used for directly searching sparse content.
   *
   * @param properties
   */
  public Query(String type, String queryString, Map<String, String> options) {
    this(queryString, options);

    this.type = type;
    this.name = null;
  }


  public Query(String name, String type, String queryString, Map<String, String> options) {
    this(type, queryString, options);
    this.name = name;
  }
  /**
   * Get the type of query this is.
   *
   * @return SOLR, SPARSE
   * @see Type
   */
  public String getType() {
    return type;
  }

  /**
   * Get the query string to be used when querying.
   *
   * @return The query string to use. Never null or blank;
   */
  public String getQueryString() {
    return queryString;
  }

  /**
   * Get the options to be applied when querying such as sorting.
   *
   * @return {@link Map} of options. null if not set.
   */
  public Map<String, String> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    String retval = "query::" + queryString;
    if (options != null) {
      retval += "; options::" + options.toString();
    }
    return retval;
  }

  public String getName() {
    return name;
  }
}
