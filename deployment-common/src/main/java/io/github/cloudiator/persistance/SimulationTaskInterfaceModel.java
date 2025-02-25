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

package io.github.cloudiator.persistance;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Entity
class SimulationTaskInterfaceModel extends TaskInterfaceModel {

  @OneToOne(optional = false)
  private DistributionModel startTime;

  protected SimulationTaskInterfaceModel() {
    
  }

  public SimulationTaskInterfaceModel(TaskModel taskModel, DistributionModel startTime) {
    super(taskModel);

    checkNotNull(startTime, "startTime is null");

    this.startTime = startTime;
  }

  public DistributionModel getStartTime() {
    return startTime;
  }

  public SimulationTaskInterfaceModel setStartTime(
      DistributionModel startTime) {
    this.startTime = startTime;
    return this;
  }
}
