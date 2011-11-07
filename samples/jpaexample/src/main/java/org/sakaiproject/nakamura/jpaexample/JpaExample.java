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
package org.sakaiproject.nakamura.jpaexample;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.jpaexample.jpa.model.ExampleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

@Component
public class JpaExample {

  private static final Logger LOG = LoggerFactory.getLogger(JpaExample.class);

  @Reference
  private EntityManager entityManager;

  public JpaExample() {
  }
  
  public void activate(ComponentContext componentContext) {
    exercise();
  }

  public void exercise() {
    LOG.info("Doing some JPA");
    LOG.info("EM: " + entityManager);

    LOG.info("Creating example model");
    ExampleModel model = new ExampleModel();
    model.setProperty("Some property");
    // entityManager.getTransaction().begin();
    entityManager.persist(model);
    // entityManager.getTransaction().commit();

    LOG.info("Attempting to read back model from database");

    // model should be written to database now.
    ExampleModel model2 = entityManager.find(ExampleModel.class, model.getId());
    LOG.info("Model " + model.getId() + " from db: " + model2);
  }

}
