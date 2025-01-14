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

package io.github.cloudiator.persistance;

import de.uniulm.omi.cloudiator.util.OneWayConverter;
import io.github.cloudiator.deployment.domain.DockerInterface;
import io.github.cloudiator.deployment.domain.DockerInterfaceBuilder;
import io.github.cloudiator.deployment.domain.FaasInterface;
import io.github.cloudiator.deployment.domain.FaasInterfaceBuilder;
import io.github.cloudiator.deployment.domain.LanceInterface;
import io.github.cloudiator.deployment.domain.LanceInterfaceBuilder;
import io.github.cloudiator.deployment.domain.SimulationInterface;
import io.github.cloudiator.deployment.domain.SimulationInterfaceImpl;
import io.github.cloudiator.deployment.domain.SparkInterface;
import io.github.cloudiator.deployment.domain.SparkInterfaceBuilder;
import io.github.cloudiator.deployment.domain.TaskInterface;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class TaskInterfaceModelConverter implements
    OneWayConverter<TaskInterfaceModel, TaskInterface> {

  private static final TriggerModelConverter TRIGGER_MODEL_CONVERTER = new TriggerModelConverter();
  private static final DistributionModelConverter DISTRIBUTION_MODEL_CONVERTER = new DistributionModelConverter();

  @Nullable
  @Override
  public TaskInterface apply(@Nullable TaskInterfaceModel taskInterfaceModel) {

    if (taskInterfaceModel == null) {
      return null;
    }

    if (taskInterfaceModel instanceof LanceTaskInterfaceModel) {
      return lanceInterface((LanceTaskInterfaceModel) taskInterfaceModel);
    } else if (taskInterfaceModel instanceof FaasTaskInterfaceModel) {
      return faasInterface((FaasTaskInterfaceModel) taskInterfaceModel);
    } else if (taskInterfaceModel instanceof DockerTaskInterfaceModel) {
      return dockerInterface((DockerTaskInterfaceModel) taskInterfaceModel);
    } else if (taskInterfaceModel instanceof SparkTaskInterfaceModel) {
      return sparkInterface((SparkTaskInterfaceModel) taskInterfaceModel);
    } else if (taskInterfaceModel instanceof SimulationTaskInterfaceModel) {
      return simulationInterface((SimulationTaskInterfaceModel) taskInterfaceModel);
    } else {
      throw new AssertionError(
          String.format("taskInterfaceModel has illegal type %s.", taskInterfaceModel.getClass()));
    }
  }

  private LanceInterface lanceInterface(LanceTaskInterfaceModel lanceTaskInterfaceModel) {
    return LanceInterfaceBuilder.newBuilder()
        .containerType(lanceTaskInterfaceModel.getLanceContainerType())
        .init(lanceTaskInterfaceModel.getInit())
        .install(lanceTaskInterfaceModel.getInstall())
        .postInstall(lanceTaskInterfaceModel.getPostInstall())
        .postStart(lanceTaskInterfaceModel.getPostStart())
        .postStop(lanceTaskInterfaceModel.getPostStop())
        .preInstall(lanceTaskInterfaceModel.getPreInstall())
        .preStart(lanceTaskInterfaceModel.getPreStart())
        .preStop(lanceTaskInterfaceModel.getPreStop())
        .shutdown(lanceTaskInterfaceModel.getShutdown())
        .start(lanceTaskInterfaceModel.getStart())
        .startDetection(lanceTaskInterfaceModel.getStartDetection())
        .stop(lanceTaskInterfaceModel.getStop())
        .stopDetection(lanceTaskInterfaceModel.getStopDetection())
        .portUpdateAction(lanceTaskInterfaceModel.getPortUpdateAction()).build();
  }

  private FaasInterface faasInterface(FaasTaskInterfaceModel faasTaskInterfaceModel) {
    return FaasInterfaceBuilder.newBuilder()
        .functionName(faasTaskInterfaceModel.getFunctionName())
        .sourceCodeUrl(faasTaskInterfaceModel.getSourceCodeUrl())
        .handler(faasTaskInterfaceModel.getHandler())
        .timeout(faasTaskInterfaceModel.getTimeout())
        .triggers(faasTaskInterfaceModel.getTriggers().stream()
            .map(TRIGGER_MODEL_CONVERTER)
            .collect(Collectors.toSet())
        )
        .functionEnvironment(faasTaskInterfaceModel.getFunctionEnvironment())
        .build();
  }

  private DockerInterface dockerInterface(DockerTaskInterfaceModel dockerTaskInterfaceModel) {
    return DockerInterfaceBuilder.newBuilder()
        .dockerImage(dockerTaskInterfaceModel.getDockerImage())
        .environment(dockerTaskInterfaceModel.getEnvVars())
        .portUpdateAction(dockerTaskInterfaceModel.getPortUpdateAction()).build();
  }

  private SparkInterface sparkInterface(SparkTaskInterfaceModel sparkTaskInterfaceModel) {
    return SparkInterfaceBuilder.newBuilder().file(sparkTaskInterfaceModel.getFile())
        .className(sparkTaskInterfaceModel.getClassName())
        .arguments(sparkTaskInterfaceModel.getArguments())
        .sparkArguments(sparkTaskInterfaceModel.getSparkArguments())
        .sparkConfiguration(sparkTaskInterfaceModel.getSparkConfiguration())
        .processMapping(sparkTaskInterfaceModel.getProcessMapping()).build();
  }

  private SimulationInterface simulationInterface(
      SimulationTaskInterfaceModel simulationTaskInterfaceModel) {
    return new SimulationInterfaceImpl(
        DISTRIBUTION_MODEL_CONVERTER.apply(simulationTaskInterfaceModel.getStartTime()));
  }
}
