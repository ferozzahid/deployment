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

package io.github.cloudiator.deployment.scheduler;

import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import io.github.cloudiator.deployment.domain.Process;
import io.github.cloudiator.deployment.domain.ProcessBuilder;
import io.github.cloudiator.deployment.domain.ProcessGroup;
import io.github.cloudiator.deployment.domain.ProcessGroupImpl;
import io.github.cloudiator.deployment.domain.Schedule;
import io.github.cloudiator.deployment.domain.Task;
import io.github.cloudiator.deployment.messaging.JobConverter;
import io.github.cloudiator.domain.Node;
import io.github.cloudiator.messaging.NodeToNodeMessageConverter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.cloudiator.messages.General.Error;
import org.cloudiator.messages.Process.CreateLanceProcessRequest;
import org.cloudiator.messages.Process.ProcessCreatedResponse;
import org.cloudiator.messages.entities.ProcessEntities;
import org.cloudiator.messages.entities.ProcessEntities.LanceProcess;
import org.cloudiator.messaging.ResponseCallback;
import org.cloudiator.messaging.ResponseException;
import org.cloudiator.messaging.services.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanceProcessSpawnerImpl implements ProcessSpawner {

  private final ProcessService processService;
  private final JobConverter jobConverter = new JobConverter();
  private final NodeToNodeMessageConverter nodeToNodeMessageConverter = new NodeToNodeMessageConverter();
  private final ResourcePool resourcePool;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(LanceProcessSpawnerImpl.class);

  @Inject
  public LanceProcessSpawnerImpl(ProcessService processService,
      ResourcePool resourcePool) {
    this.processService = processService;
    this.resourcePool = resourcePool;
  }

  @Override
  public boolean supports(Task task) {
    //for the time being we only have lance so it supports everything
    //todo implement this correctly
    return true;
  }

  @Override
  public ProcessGroup spawn(String userId, Schedule schedule, Task task) {

    try {

      //allocate the required resources from the resource pool
      final Iterable<Node> allocatedNodes = resourcePool.allocate(userId, task.requirements());

      List<Process> processList = new LinkedList<>();
      for (Node node : allocatedNodes) {
        final LanceProcess lanceProcess = LanceProcess.newBuilder()
            .setSchedule(
                ProcessEntities.Schedule.newBuilder().setId(schedule.id())
                    .setJob(schedule.job().id())
                    .build())
            .setJob(jobConverter.applyBack(schedule.job()))
            .setNode(nodeToNodeMessageConverter.apply(node)).setTask(task.name()).build();
        final CreateLanceProcessRequest processRequest = CreateLanceProcessRequest.newBuilder()
            .setLance(lanceProcess).setUserId(userId).build();

        SettableFuture<ProcessCreatedResponse> createResponseFuture = SettableFuture.create();
        processService
            .createLanceProcessAsync(processRequest,
                new ResponseCallback<ProcessCreatedResponse>() {
                  @Override
                  public void accept(@Nullable ProcessCreatedResponse processCreatedResponse,
                      @Nullable Error error) {
                    if (processCreatedResponse != null) {
                      createResponseFuture.set(processCreatedResponse);
                    } else {
                      if (error == null) {
                        throw new IllegalStateException("Expected error to be set, but was null");
                      }
                      createResponseFuture
                          .setException(new ResponseException(error.getCode(), error.getMessage()));
                    }
                  }
                });

        for (ProcessEntities.Process process : createResponseFuture.get().getProcessGroup()
            .getProcessesList()) {
          processList.add(
              ProcessBuilder.newBuilder().id(process.getId()).taskName(process.getTask())
                  .jobId(process.getJob()).build());
        }
      }
      return new ProcessGroupImpl(processList);
    } catch (ResponseException e) {
      throw new IllegalStateException("Could not create process", e);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Thread got interrupted while waiting for lance response", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Could not create process", e.getCause());
    }
  }
}