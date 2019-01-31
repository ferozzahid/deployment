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

package io.github.cloudiator.deployment.messaging;

import com.google.common.base.Strings;
import de.uniulm.omi.cloudiator.util.TwoWayConverter;
import io.github.cloudiator.deployment.domain.CloudiatorClusterProcess;
import io.github.cloudiator.deployment.domain.CloudiatorClusterProcessBuilder;
import io.github.cloudiator.deployment.domain.CloudiatorProcess;
import io.github.cloudiator.deployment.domain.CloudiatorProcess.Type;
import io.github.cloudiator.deployment.domain.CloudiatorSingleProcess;
import io.github.cloudiator.deployment.domain.CloudiatorSingleProcessBuilder;
import org.cloudiator.messages.entities.ProcessEntities;
import org.cloudiator.messages.entities.ProcessEntities.Process;
import org.cloudiator.messages.entities.ProcessEntities.Process.Builder;
import org.cloudiator.messages.entities.ProcessEntities.ProcessState;
import org.cloudiator.messages.entities.ProcessEntities.ProcessType;

public class ProcessMessageConverter implements
    TwoWayConverter<ProcessEntities.Process, CloudiatorProcess> {

  public static final ProcessMessageConverter INSTANCE = new ProcessMessageConverter();
  public static final ProcessStateConverter PROCESS_STATE_CONVERTER = ProcessStateConverter.INSTANCE;


  private ProcessMessageConverter() {

  }

  @Override
  public Process applyBack(CloudiatorProcess cloudiatorProcess) {

    if (cloudiatorProcess instanceof CloudiatorSingleProcess) {
      //Lance, Docker and FaaS processes

      final Builder builder = Process.newBuilder()
          .setNode(((CloudiatorSingleProcess) cloudiatorProcess).node());

      return finishBuilding(cloudiatorProcess, builder);


    } else if (cloudiatorProcess instanceof CloudiatorClusterProcess) {
      //Spark processes
      final Builder builder = Process.newBuilder()
          .setNodeGroup(((CloudiatorClusterProcess) cloudiatorProcess).nodeGroup());

      return finishBuilding(cloudiatorProcess, builder);

    } else {
      throw new IllegalStateException(
          "Unknown CloudiatorProcess interface: " + cloudiatorProcess.getClass().getName());
    }
  }

  private Process finishBuilding(CloudiatorProcess cloudiatorProcess, Builder builder) {
    builder.setId(cloudiatorProcess.id()).setUserId(cloudiatorProcess.userId())
        .setSchedule(cloudiatorProcess.scheduleId()).setTask(cloudiatorProcess.taskId())
        .setType(ProcessTypeConverter.INSTANCE.applyBack(cloudiatorProcess.type()))
        .setState(PROCESS_STATE_CONVERTER.applyBack(cloudiatorProcess.state()))
    ;

    if (cloudiatorProcess.reason().isPresent()) {
      builder.setReason(cloudiatorProcess.reason().get());
    }

    if (cloudiatorProcess.diagnostic().isPresent()) {
      builder.setDiagnostic(cloudiatorProcess.diagnostic().get());
    }

    return builder.build();
  }

  @Override
  public CloudiatorProcess apply(Process process) {

    switch (process.getRunsOnCase()) {
      case NODE:
        final CloudiatorSingleProcessBuilder cloudiatorSingleProcessBuilder = CloudiatorSingleProcessBuilder
            .create()
            .id(process.getId())
            .userId(process.getUserId())
            .scheduleId(process.getSchedule())
            .taskName(process.getTask())
            .node(process.getNode())
            .state(PROCESS_STATE_CONVERTER.apply(process.getState()))
            .type(ProcessTypeConverter.INSTANCE.apply(process.getType()));

        if (!Strings.isNullOrEmpty(process.getDiagnostic())) {
          cloudiatorSingleProcessBuilder.diagnostic(process.getDiagnostic());
        }

        if (Strings.isNullOrEmpty(process.getReason())) {
          cloudiatorSingleProcessBuilder.reason(process.getReason());
        }

        return cloudiatorSingleProcessBuilder.build();

      case NODEGROUP:
        final CloudiatorClusterProcessBuilder cloudiatorClusterProcessBuilder = CloudiatorClusterProcessBuilder
            .create()
            .id(process.getId())
            .userId(process.getUserId())
            .scheduleId(process.getSchedule())
            .taskName(process.getTask())
            .nodeGroup(process.getNodeGroup())
            .state(PROCESS_STATE_CONVERTER.apply(process.getState()))
            .type(ProcessTypeConverter.INSTANCE.apply(process.getType()));

        if (!Strings.isNullOrEmpty(process.getDiagnostic())) {
          cloudiatorClusterProcessBuilder.diagnostic(process.getDiagnostic());
        }

        if (Strings.isNullOrEmpty(process.getReason())) {
          cloudiatorClusterProcessBuilder.reason(process.getReason());
        }

        return cloudiatorClusterProcessBuilder.build();

      case RUNSON_NOT_SET:
        throw new IllegalStateException("RunsOn parameter not set for process: " + process.getId());

      default:
        throw new AssertionError("Unknown process: " + process);
    }


  }

  public static class ProcessStateConverter implements
      TwoWayConverter<ProcessEntities.ProcessState, CloudiatorProcess.ProcessState> {

    private static final ProcessStateConverter INSTANCE = new ProcessStateConverter();

    private ProcessStateConverter() {

    }

    @Override
    public ProcessState applyBack(CloudiatorProcess.ProcessState processState) {
      switch (processState) {
        case DELETED:
          return ProcessState.PROCESS_STATE_DELETED;
        case CREATED:
          return ProcessState.PROCESS_STATE_CREATED;
        case ERROR:
          return ProcessState.PROCESS_STATE_ERROR;
        case RUNNING:
          return ProcessState.PROCESS_STATE_FAILED;
        case FAILED:
          return ProcessState.PROCESS_STATE_FAILED;
        case FINISHED:
          return ProcessState.PROCESS_STATE_FINISHED;
        default:
          throw new AssertionError("Unknown processState: " + processState);
      }
    }

    @Override
    public CloudiatorProcess.ProcessState apply(ProcessState processState) {
      switch (processState) {
        case PROCESS_STATE_FAILED:
          return CloudiatorProcess.ProcessState.FAILED;
        case PROCESS_STATE_ERROR:
          return CloudiatorProcess.ProcessState.ERROR;
        case PROCESS_STATE_CREATED:
          return CloudiatorProcess.ProcessState.CREATED;
        case PROCESS_STATE_DELETED:
          return CloudiatorProcess.ProcessState.DELETED;
        case PROCESS_STATE_RUNNING:
          return CloudiatorProcess.ProcessState.RUNNING;
        case PROCESS_STATE_FINISHED:
          return CloudiatorProcess.ProcessState.FINISHED;
        case UNRECOGNIZED:
        default:
          throw new AssertionError("Unknown or illegal process state " + processState);
      }
    }
  }

  private static class ProcessTypeConverter implements
      TwoWayConverter<ProcessEntities.ProcessType, CloudiatorProcess.Type> {

    private static final ProcessTypeConverter INSTANCE = new ProcessTypeConverter();

    @Override
    public ProcessType applyBack(Type type) {
      switch (type) {
        case LANCE:
          return ProcessType.LANCE;
        case SPARK:
          return ProcessType.SPARK;
        default:
          throw new AssertionError("Unknown type: " + type);
      }
    }

    @Override
    public Type apply(ProcessType processType) {
      switch (processType) {
        case SPARK:
          return Type.SPARK;
        case LANCE:
          return Type.LANCE;
        case UNRECOGNIZED:
        default:
          throw new AssertionError("Unknown process type: " + processType);
      }
    }
  }
}
