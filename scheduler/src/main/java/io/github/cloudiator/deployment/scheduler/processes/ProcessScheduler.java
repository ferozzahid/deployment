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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import io.github.cloudiator.deployment.domain.CloudiatorClusterProcess;
import io.github.cloudiator.deployment.domain.CloudiatorClusterProcessBuilder;
import io.github.cloudiator.deployment.domain.CloudiatorProcess;
import io.github.cloudiator.deployment.domain.CloudiatorProcess.ProcessState;
import io.github.cloudiator.deployment.domain.CloudiatorSingleProcess;
import io.github.cloudiator.deployment.domain.CloudiatorSingleProcessBuilder;
import io.github.cloudiator.deployment.domain.Environment;
import io.github.cloudiator.deployment.domain.EnvironmentGenerator;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.Schedule;
import io.github.cloudiator.deployment.domain.Task;
import io.github.cloudiator.deployment.domain.TaskInterface;
import io.github.cloudiator.deployment.messaging.JobMessageRepository;
import io.github.cloudiator.deployment.scheduler.instantiation.TaskInterfaceSelection;
import io.github.cloudiator.deployment.scheduler.messaging.ProcessRequestSubscriber;
import io.github.cloudiator.domain.Node;
import io.github.cloudiator.domain.NodeState;
import io.github.cloudiator.messaging.NodeMessageRepository;
import io.github.cloudiator.persistance.ScheduleDomainRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessScheduler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ProcessRequestSubscriber.class);
  private final ScheduleDomainRepository scheduleDomainRepository;
  private final JobMessageRepository jobMessageRepository;
  private final ProcessSpawner processSpawner;
  private final NodeMessageRepository nodeMessageRepository;

  @Inject
  public ProcessScheduler(
      ScheduleDomainRepository scheduleDomainRepository,
      JobMessageRepository jobMessageRepository,
      ProcessSpawner processSpawner,
      NodeMessageRepository nodeMessageRepository) {
    this.scheduleDomainRepository = scheduleDomainRepository;
    this.jobMessageRepository = jobMessageRepository;
    this.processSpawner = processSpawner;
    this.nodeMessageRepository = nodeMessageRepository;
  }

  @SuppressWarnings("WeakerAccess")
  @Transactional
  Schedule retrieveSchedule(String scheduleId, String userId) {
    return scheduleDomainRepository.findByIdAndUser(scheduleId, userId);
  }

  public CloudiatorProcess schedule(CloudiatorProcess cloudiatorProcess)
      throws ProcessSpawningException {

    if (cloudiatorProcess.state() != ProcessState.PENDING) {
      throw new ProcessSpawningException(String
          .format("Expected process %s to be in state pending, was in state %s", cloudiatorProcess,
              cloudiatorProcess.state()));
    }

    LOGGER.debug(String.format("Retrieving schedule for process %s.", cloudiatorProcess));
    final Schedule schedule = retrieveSchedule(cloudiatorProcess.scheduleId(),
        cloudiatorProcess.userId());

    if (schedule == null) {
      throw new ProcessSpawningException(String
          .format("Illegal schedule. Schedule with id %s does not exist.",
              cloudiatorProcess.scheduleId()));
    }

    final Job job = jobMessageRepository.getById(cloudiatorProcess.userId(), schedule.job());

    if (job == null) {
      throw new ProcessSpawningException(String
          .format("Illegal schedule. Job with id %s does not exist.",
              schedule.job()));
    }

    final Optional<Task> optionalTask = job.getTask(cloudiatorProcess.taskId());
    if (!optionalTask.isPresent()) {
      throw new ProcessSpawningException(String
          .format("Illegal process. Job with id %s does not have task %s.",
              schedule.job(), cloudiatorProcess.taskId()));
    }

    final TaskInterface taskInterface = new TaskInterfaceSelection()
        .select(optionalTask.get());

    //decorate the environment
    final Environment environment = EnvironmentGenerator.of(job, schedule)
        .generate(cloudiatorProcess);

    LOGGER.debug(
        String.format("Generated environment %s for process %s", environment, cloudiatorProcess));

    if (cloudiatorProcess instanceof CloudiatorSingleProcess) {

      final Node node = getNode(cloudiatorProcess.userId(),
          ((CloudiatorSingleProcess) cloudiatorProcess).node());

      if (!node.state().equals(NodeState.RUNNING)) {
        throw new ProcessSpawningException(
            String.format("Node %s is in illegal state %s.", node, node.state()));
      }

      final CloudiatorSingleProcess spawned = processSpawner
          .spawn(cloudiatorProcess.userId(), schedule.id(), job, optionalTask.get(),
              taskInterface.decorateEnvironment(environment),
              node);

      return CloudiatorSingleProcessBuilder.of((CloudiatorSingleProcess) cloudiatorProcess)
          .state(ProcessState.RUNNING)
          .originId(spawned.originId().orElse(null)).type(spawned.type())
          .endpoint(spawned.endpoint().orElse(null))
          .addAllIpAddresses(node.ipAddresses())
          .build();


    } else if (cloudiatorProcess instanceof CloudiatorClusterProcess) {

      final Set<Node> nodeSet = cloudiatorProcess.nodes().stream().map(
          s -> getNode(cloudiatorProcess.userId(), s)).collect(Collectors.toSet());

      for (Node node : nodeSet) {
        if (!node.state().equals(NodeState.RUNNING)) {
          throw new ProcessSpawningException(
              String.format("Node %s is in illegal state %s.", node, node.state()));
        }
      }

      final CloudiatorClusterProcess spawned = processSpawner
          .spawn(cloudiatorProcess.userId(), schedule.id(), job, optionalTask.get(),
              taskInterface.decorateEnvironment(environment),
              nodeSet);

      return CloudiatorClusterProcessBuilder.of((CloudiatorClusterProcess) cloudiatorProcess)
          .state(ProcessState.RUNNING)
          .originId(spawned.originId().orElse(null)).type(spawned.type())
          .endpoint(spawned.endpoint().orElse(null))
          .build();

    } else {
      throw new AssertionError(
          "Unknown cloudiatorProcess type " + cloudiatorProcess.getClass().getName());
    }


  }

  private Node getNode(String userId, String id) {

    final Retryer<Node> nodeRetryer = RetryerBuilder.<Node>newBuilder()
        .withWaitStrategy(WaitStrategies.randomWait(30, TimeUnit.SECONDS)).retryIfResult(
            Objects::isNull)
        .retryIfResult(input -> input != null && input.state().equals(NodeState.PENDING))
        .withStopStrategy(StopStrategies.stopAfterDelay(2, TimeUnit.MINUTES)).build();

    try {
      return nodeRetryer.call(() -> nodeMessageRepository.getById(userId, id));
    } catch (ExecutionException e) {
      LOGGER.warn("Catching node with id %s failed.", e.getCause());
    } catch (RetryException e) {
      LOGGER.warn("Catching node with id %s failed.", e);
    }

    return nodeMessageRepository.getById(userId, id);
  }


}
