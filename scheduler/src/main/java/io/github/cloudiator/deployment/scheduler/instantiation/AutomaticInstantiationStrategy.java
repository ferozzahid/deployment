/*
 * Copyright 2018 University of Ulm
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

package io.github.cloudiator.deployment.scheduler.instantiation;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import de.uniulm.omi.cloudiator.util.execution.LoggingScheduledThreadPoolExecutor;
import io.github.cloudiator.deployment.domain.CloudiatorProcess;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.Schedule;
import io.github.cloudiator.deployment.domain.Schedule.Instantiation;
import io.github.cloudiator.deployment.domain.Task;
import io.github.cloudiator.deployment.messaging.ProcessMessageConverter;
import io.github.cloudiator.deployment.scheduler.ResourcePool;
import io.github.cloudiator.domain.Node;
import io.github.cloudiator.domain.NodeGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.cloudiator.messages.Process.CreateProcessRequest;
import org.cloudiator.messages.Process.ProcessCreatedResponse;
import org.cloudiator.messages.entities.ProcessEntities.ProcessNew;
import org.cloudiator.messaging.SettableFutureResponseCallback;
import org.cloudiator.messaging.services.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticInstantiationStrategy implements InstantiationStrategy {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AutomaticInstantiationStrategy.class);
  private static final ProcessMessageConverter PROCESS_MESSAGE_CONVERTER = ProcessMessageConverter.INSTANCE;
  private static final LoggingScheduledThreadPoolExecutor EXECUTOR = new LoggingScheduledThreadPoolExecutor(
      5);

  static {
    MoreExecutors.addDelayedShutdownHook(EXECUTOR, 5, TimeUnit.MINUTES);
  }

  private final ResourcePool resourcePool;
  private final ProcessService processService;

  @Inject
  public AutomaticInstantiationStrategy(
      ResourcePool resourcePool, ProcessService processService) {
    this.resourcePool = resourcePool;
    this.processService = processService;
  }


  @Override
  public boolean supports(Instantiation instantiation) {
    return instantiation.equals(Instantiation.AUTOMATIC);
  }

  @Override
  public void instantiate(Schedule schedule, Job job, String userId) throws InstantiationException {

    checkState(supports(schedule.instantiation()),
        String.format("%s does not support instantiation %s.", this, schedule.instantiation()));

    //futures for the process
    final Set<ListenableFuture<CloudiatorProcess>> processFutures = new HashSet<>();

    //countdown latch
    CountDownLatch countDownLatch = new CountDownLatch(job.tasks().size());

    //for each task
    for (Task task : job.tasks()) {

      LOGGER.info(String.format("Allocating the resources for task %s of job %s", task, job));

      //allocate the resources
      final ListenableFuture<NodeGroup> allocateFuture = resourcePool
          .allocate(userId, task.requirements(), task.name());

      Futures.addCallback(allocateFuture, new FutureCallback<NodeGroup>() {
        @Override
        public void onSuccess(@Nullable NodeGroup result) {

          LOGGER.info(String.format("Node group %s was created successfully. Starting processes.",
              result));

          //spawn processes on success
          for (Node node : result.getNodes()) {

            LOGGER.info(String
                .format("Requesting new process for schedule %s, task %s on node %s.", schedule,
                    task, node));

            final ProcessNew newProcess = ProcessNew.newBuilder().setSchedule(
                schedule.id()).setTask(task.name())
                .setNode(node.id())
                .build();
            final CreateProcessRequest createProcessRequest = CreateProcessRequest
                .newBuilder()
                .setUserId(userId).setProcess(newProcess).build();

            final SettableFutureResponseCallback<ProcessCreatedResponse, CloudiatorProcess> processFuture = SettableFutureResponseCallback
                .create(processCreatedResponse -> PROCESS_MESSAGE_CONVERTER
                    .apply(processCreatedResponse.getProcess()));

            processService.createProcessAsync(createProcessRequest, processFuture);

            processFutures.add(processFuture);

            countDownLatch.countDown();

          }
        }

        @Override
        public void onFailure(Throwable t) {
          //todo: what to do when nodes fail?
          LOGGER.error(String
                  .format("An uncaught exception occurred while spawning nodes: %s.", t.getMessage()),
              t);

          countDownLatch.countDown();

        }
      }, EXECUTOR);

    }

    try {

      LOGGER.info("Waiting for knowledge about final process sizes.");
      countDownLatch.await();

      LOGGER.info(String.format("Creating %s processes for job %s.", processFutures.size(), job));

      final ListenableFuture<List<CloudiatorProcess>> processes = Futures
          .successfulAsList(processFutures);

      LOGGER.info(String.format("Waiting for %s processes of job %s.", processFutures.size(), job));
      final List<CloudiatorProcess> cloudiatorProcesses = processes.get();
      if (cloudiatorProcesses.contains(null)) {
        throw new IllegalStateException("One or more process failed to start");
      }
    } catch (InterruptedException e) {
      throw new InstantiationException("Execution got interrupted.", e);
    } catch (ExecutionException e) {
      throw new InstantiationException("Error during instantiation.", e);
    }

    LOGGER.info(String.format("Finished instantiation of job %s.", job));

  }
}
