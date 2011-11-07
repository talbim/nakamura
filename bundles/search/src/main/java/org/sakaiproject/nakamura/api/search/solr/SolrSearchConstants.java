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

public interface SolrSearchConstants {

  /**
   * The default amount of items in a page.
   */
  public static final int DEFAULT_PAGED_ITEMS = 25;
  /**
   * The default page to start on.
   */
  public static final int DEFAULT_PAGE = 0;
  /**
   * The frequency of the aggregate term.
   */
  public static final String JSON_COUNT = "count";
  /**
   * The name of the aggregate term.
   */
  public static final String JSON_NAME = "name";
  /**
  *
  */
  public static final String JSON_RESULTS = "results";
  /**
   * Holder for the totals of aggregate calculations.
   */
  public static final String JSON_TOTALS = "totals";
  /**
  *
  */
  public static final String PARAMS_ITEMS_PER_PAGE = "items";
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";
  /**
   *
   */
  public static final String REG_PROCESSOR_NAMES = "sakai.search.processor";
  /**
   *
   */
  public static final String REG_BATCH_PROCESSOR_NAMES = "sakai.search.batchprocessor";

  /**
   *
   */
  public static final String REG_PROVIDER_NAMES = "sakai.search.provider";
  /**
  *
  */
  public static final String SAKAI_QUERY_LANGUAGE = "sakai:query-language";
  /**
   * Property name of the query template
   */
  public static final String SAKAI_QUERY_TEMPLATE = "sakai:query-template";
  /**
   * Property name of options to be applied to a query
   */
  public static final String SAKAI_QUERY_TEMPLATE_OPTIONS = "sakai:query-template-options";
  /**
   * Property that defines whether the results should have a limit on it.
   */
  public static final String SAKAI_LIMIT_RESULTS = "sakai:limit-results";
  /**
   *
   */
  public static final String SAKAI_PROPERTY_PROVIDER = "sakai:propertyprovider";
  /**
   *
   */
  public static final String SAKAI_RESULTPROCESSOR = "sakai:resultprocessor";
  /**
   *
   */
  public static final String SAKAI_BATCHRESULTPROCESSOR = "sakai:batchresultprocessor";
  /**
  *
  */
  public static final String SEARCH_RESULT_PROCESSOR = "SearchResultProcessor";
  /**
  *
  */
  public static final String SEARCH_BATCH_RESULT_PROCESSOR = "SearchBatchResultProcessor";
  /**
   * The path where the search templates have to be under to be executable.
   */
  public static final String SEARCH_PATH_PREFIX = "/var";
  /**
  *
  */
  public static final String SEARCH_PROPERTY_PROVIDER = "SearchPropertyProvider";
  /**
  *
  */
  public static final String TOTAL = "total";
  /**
   *
   */
  public static final String TIDY = "tidy";
  /**
   *
   */
  public static final String INFINITY = "infinity";
}
