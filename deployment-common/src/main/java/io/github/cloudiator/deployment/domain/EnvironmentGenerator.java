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

package io.github.cloudiator.deployment.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import io.github.cloudiator.deployment.graph.Graphs;
import io.github.cloudiator.deployment.graph.ScheduleGraph;
import io.github.cloudiator.deployment.graph.ScheduleGraph.CommunicationInstanceEdge;

public class EnvironmentGenerator {

  private static final String PUBLIC_DOWNSTREAM = "PUBLIC_%s";

  private final Job job;
  private final Schedule schedule;

  private EnvironmentGenerator(Job job, Schedule schedule) {
    this.job = job;
    this.schedule = schedule;
  }

  public static EnvironmentGenerator of(Job job, Schedule schedule) {
    checkNotNull(job, "job is null");
    checkNotNull(schedule, "schedule is null");

    return new EnvironmentGenerator(job, schedule);
  }


  public Environment generate(CloudiatorProcess cloudiatorProcess) {

    Environment environment = new Environment();
    final ScheduleGraph scheduleGraph = Graphs.scheduleGraph(schedule, job);

    for (CloudiatorProcess dependency : scheduleGraph.getDependencies(cloudiatorProcess)) {

      final CommunicationInstanceEdge edge = scheduleGraph.getEdge(dependency, cloudiatorProcess);
      if (dependency.endpoint().isPresent()) {
        environment.put(String.format(PUBLIC_DOWNSTREAM, edge.getCommunication().portRequired()),
            dependency.endpoint().get());
      }
    }

    return environment;

  }

}
