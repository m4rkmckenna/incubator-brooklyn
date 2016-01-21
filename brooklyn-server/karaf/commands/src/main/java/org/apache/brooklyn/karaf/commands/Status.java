/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.brooklyn.karaf.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.brooklyn.api.mgmt.ManagementContext;
/**
 * @author m4rkmckenna on 21/01/2016.
 */
@Service
@Command(scope = "bk", name = "status",description = "Brooklyn status")
public class Status implements Action {

    @Reference
    private ManagementContext managementContext;


    @Override
    public Object execute() throws Exception {
        System.out.println("Brooklyn Server Status");
        System.out.println(String.format("Startup Complete [%s]",managementContext.isStartupComplete()));
        System.out.println(String.format("Running [%s]",managementContext.isRunning()));
        System.out.println(String.format("Configured locations [%s]",managementContext.getLocationRegistry().getDefinedLocations().size()));
        return null;
    }
}
