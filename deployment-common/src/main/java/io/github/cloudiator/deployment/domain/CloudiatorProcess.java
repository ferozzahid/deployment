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

import de.uniulm.omi.cloudiator.domain.Identifiable;
import de.uniulm.omi.cloudiator.sword.domain.IpAddress;
import de.uniulm.omi.cloudiator.util.stateMachine.State;
import de.uniulm.omi.cloudiator.util.stateMachine.Stateful;
import io.github.cloudiator.deployment.domain.CloudiatorProcess.ProcessState;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * Created by daniel on 13.02.17.
 */
public interface CloudiatorProcess extends Identifiable, Stateful<ProcessState> {

  enum ProcessState implements State {
    PENDING,
    RUNNING,
    FINISHED,
    ERROR,
    DELETED
  }

  enum Type {
    LANCE,
    SPARK,
    FAAS,
    SIMULATION,
    UNKNOWN
  }

  Optional<String> originId();

  String scheduleId();

  String taskId();

  String taskInterface();

  ProcessState state();

  Type type();

  String userId();

  Optional<String> diagnostic();

  Optional<String> reason();

  Set<String> nodes();

  Optional<String> endpoint();

  Collection<IpAddress> ipAddresses();

  Date start();

  Optional<Date> stop();

}
