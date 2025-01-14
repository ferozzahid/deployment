/*
 * Copyright 2014-2019 University of Ulm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.cloudiator.deployment.scheduler.processes;

import io.github.cloudiator.deployment.domain.CloudiatorClusterProcess;
import io.github.cloudiator.deployment.domain.CloudiatorSingleProcess;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.Task;
import io.github.cloudiator.deployment.domain.TaskInterface;
import io.github.cloudiator.domain.Node;
import java.util.Set;


public interface ProcessSpawner {

  boolean supports(TaskInterface taskInterface);

  CloudiatorSingleProcess spawn(String userId, String schedule, Job job, Task task,
      TaskInterface taskInterface, Node node) throws ProcessSpawningException;

  CloudiatorClusterProcess spawn(String userId, String schedule, Job job, Task task,
      TaskInterface taskInterface, Set<Node> nodes) throws ProcessSpawningException;
}
