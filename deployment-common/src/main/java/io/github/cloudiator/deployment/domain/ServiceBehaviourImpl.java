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

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class ServiceBehaviourImpl implements ServiceBehaviour {

  private final boolean restart;

  ServiceBehaviourImpl(boolean restart) {
    this.restart = restart;
  }

  @Override
  public boolean restart() {
    return restart;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("restart", restart).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceBehaviourImpl that = (ServiceBehaviourImpl) o;
    return restart == that.restart;
  }

  @Override
  public int hashCode() {
    return Objects.hash(restart);
  }
}
