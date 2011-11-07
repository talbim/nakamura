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
package org.sakaiproject.nakamura.memory;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.util.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

/**
 * The <code>CacheManagerServiceImpl</code>
 */
@Component(immediate = true, label = "CacheManagerServiceImpl", description = "Implementation of the Cache Manager Service")
@Service
public class CacheManagerServiceImpl implements CacheManagerService {

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Cache Manager Service Implementation")
  static final String SERVICE_DESCRIPTION = "service.description";

  private static final String CONFIG_PATH = "res://org/sakaiproject/nakamura/memory/ehcacheConfig.xml";
  private static final Logger logger = LoggerFactory.getLogger(CacheManagerServiceImpl.class);
  private CacheManager cacheManager;
  private Map<String, Cache<?>> caches = new HashMap<String, Cache<?>>();
  private ThreadLocalCacheMap requestCacheMapHolder = new ThreadLocalCacheMap();
  private ThreadLocalCacheMap threadCacheMapHolder = new ThreadLocalCacheMap();

  public CacheManagerServiceImpl() throws IOException {
    create();
  }

  private void create() throws IOException {
    logger.info("Loading Resource using "+this.getClass().getClassLoader());
    logger.info("Locally Stream was "+this.getClass().getClassLoader().getResourceAsStream(CONFIG_PATH));
    InputStream in = ResourceLoader.openResource(CONFIG_PATH, this.getClass().getClassLoader());
    cacheManager = new CacheManager(in);
    in.close();

    /*
     * Add in a shutdown hook, for safety
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      /*
       * (non-Javadoc)
       *
       * @see java.lang.Thread#run()
       */
      @Override
      public void run() {
        try {
          CacheManagerServiceImpl.this.stop();
        } catch (Throwable t) {

          // I really do want to swallow this, and make the shutdown clean for
          // others
        }
      }
    });

    // register the cache manager with JMX
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ManagementService.registerMBeans(cacheManager, mBeanServer, true, true,
        true, true);

  }

  /**
   * perform a shutdown
   */
  public void stop() {
    cacheManager.shutdown();
    // we really want to notify all threads that have maps
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.memory.CacheManagerService#getCache(java.lang.String)
   */
  public <V> Cache<V> getCache(String name, CacheScope scope) {
    switch (scope) {
    case INSTANCE:
      return getInstanceCache(name);
    case CLUSTERINVALIDATED:
      return getInstanceCache(name);
    case CLUSTERREPLICATED:
      return getInstanceCache(name);
    case REQUEST:
      return getRequestCache(name);
    case THREAD:
      return getThreadCache(name);
    default:
      return getInstanceCache(name);
    }
  }

  /**
   * Generate a cache bound to the thread.
   *
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  private <V> Cache<V> getThreadCache(String name) {
    Map<String, Cache<?>> threadCacheMap = threadCacheMapHolder.get();
    Cache<V> threadCache = (Cache<V>) threadCacheMap.get(name);
    if (threadCache == null) {
      threadCache = new MapCacheImpl<V>();
      threadCacheMap.put(name, threadCache);
    }
    return threadCache;
  }

  /**
   * Generate a cache bound to the request
   *
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  private <V> Cache<V> getRequestCache(String name) {
    Map<String, Cache<?>> requestCacheMap = requestCacheMapHolder.get();
    Cache<V> requestCache = (Cache<V>) requestCacheMap.get(name);
    if (requestCache == null) {
      requestCache = new MapCacheImpl<V>();
      requestCacheMap.put(name, requestCache);
    }
    return requestCache;
  }

  /**
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  private <V> Cache<V> getInstanceCache(String name) {
    if (name == null) {
      return new CacheImpl<V>(cacheManager, null);
    } else {
      Cache<V> c = (Cache<V>) caches.get(name);
      if (c == null) {
        c = new CacheImpl<V>(cacheManager, name);
        caches.put(name, c);
      }
      return c;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.memory.CacheManagerService#unbind(org.sakaiproject.nakamura.api.memory.CacheScope)
   */
  public void unbind(CacheScope scope) {
    switch (scope) {
    case REQUEST:
      unbindRequest();
      break;
    case THREAD:
      unbindThread();
      break;
    }
  }

  /**
   *
   */
  private void unbindThread() {
    Map<String, Cache<?>> threadCache = threadCacheMapHolder.get();
    for (Cache<?> cache : threadCache.values()) {
      cache.clear();
    }
    threadCacheMapHolder.remove();
  }

  /**
   *
   */
  private void unbindRequest() {
    Map<String, Cache<?>> requestCache = requestCacheMapHolder.get();
    for (Cache<?> cache : requestCache.values()) {
      cache.clear();
    }
    requestCacheMapHolder.remove();
  }

}
