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
package org.apache.felix.http.jetty.internal;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class NakamuraJettyManagedService implements ServiceFactory
{

    private final NakamuraJettyService jettyService;

    NakamuraJettyManagedService(final NakamuraJettyService jettyService)
    {
        this.jettyService = jettyService;
    }

    public Object getService(Bundle bundle, ServiceRegistration registration)
    {
        return new ManagedService()
        {
            @SuppressWarnings("rawtypes")
            public void updated(Dictionary properties)
            {
                jettyService.updated(properties);
            }
        };
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
    {
        // just have the reference dropped, nothing to cleanup
    }

}
