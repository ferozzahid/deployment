/*
 * Copyright 2017 University of Ulm
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

package io.github.cloudiator.deployment.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import de.uniulm.omi.cloudiator.domain.Identifiable;
import de.uniulm.omi.cloudiator.util.StreamUtil;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.cloudiator.matchmaking.domain.Requirement;

/**
 * Created by daniel on 13.02.17.
 */
public interface Job extends JobNew, Identifiable {

  default Optional<Task> getTask(String name) {
    checkNotNull(name, "name is null");
    return tasks().stream().filter(task -> task.name().equals(name)).collect(StreamUtil.getOnly());
  }

  String userId();

  Task providingTask(Communication communication);

  Task requiredTask(Communication communication);

  Set<Task> consumedBy(Task task);

  PortRequired requiredPort(Communication communication);

  PortProvided providedPort(Communication communication);

  PortProvided getProvidingPort(PortRequired portRequired);

  Set<Communication> attachedCommunications(Port port);

  Iterator<Task> tasksInOrder();
}
