/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ratis.retry;

import org.apache.ratis.util.Preconditions;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exception dependent retry policy.
 *
 * If exception is defined in policyMap, will use the retry policy
 * configured to that exception or else will use the default policy.
 */
public final class ExceptionDependentRetry implements RetryPolicy {

  public static class Builder {
    private RetryPolicy defaultPolicy;
    private final Map<String, RetryPolicy> exceptionNameToPolicyMap =
        new TreeMap<>();

    public Builder setExceptionToPolicy(Class<? extends Throwable> exception,
        RetryPolicy retryPolicy) {
      Preconditions.assertTrue(retryPolicy != null, "Exception to policy should not be null");
      final RetryPolicy previous = exceptionNameToPolicyMap.put(exception.getName(), retryPolicy);
      Preconditions.assertNull(previous, () -> "The exception " + exception + " is already set to " + previous);
      return this;
    }

    public Builder setDefaultPolicy(RetryPolicy retryPolicy) {
      Preconditions.assertTrue(retryPolicy != null, "Default Policy should not be null");
      this.defaultPolicy = retryPolicy;
      return this;
    }

    public ExceptionDependentRetry build() {
      return new ExceptionDependentRetry(defaultPolicy,
          exceptionNameToPolicyMap);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  private final RetryPolicy defaultPolicy;
  private final Map<String, RetryPolicy> exceptionNameToPolicyMap;


  private ExceptionDependentRetry(RetryPolicy defaultPolicy, Map<String, RetryPolicy> policyMap) {
    Preconditions.assertTrue(defaultPolicy != null, "Default Policy should not be null");
    this.defaultPolicy = defaultPolicy;
    this.exceptionNameToPolicyMap = Collections.unmodifiableMap(policyMap);
  }

  @Override
  public Action handleAttemptFailure(Event event) {
    RetryPolicy policy = null;

    // If exception is defined in policy map use that or else go with the
    // default one. We go with default one in 2 cases.
    // 1. If policy map does not have exception mapped to policy.
    // 2. If event has exception value null.
    if (event.getCause() != null)  {
      policy = exceptionNameToPolicyMap.get(event.getCause().getClass().getName());
    }

    if (policy == null) {
      policy = defaultPolicy;
    }

    return policy.handleAttemptFailure(event);
  }
}
