/*
 * Copyright (c) 2014-2016 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.cloudiator.deployment.lance;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import models.Application;

import java.util.function.Function;

/**
 * Created by daniel on 11.10.16.
 */
public class ApplicationToApplicationId implements Function<Application, ApplicationId> {

    @Override public ApplicationId apply(Application application) {
        return ApplicationId.fromString(application.getUuid());
    }
}
