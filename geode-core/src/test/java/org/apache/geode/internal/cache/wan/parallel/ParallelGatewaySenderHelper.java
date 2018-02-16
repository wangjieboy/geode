/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache.wan.parallel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.apache.geode.CancelCriterion;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.internal.cache.RegionQueue;
import org.apache.geode.internal.cache.wan.AbstractGatewaySender;

public class ParallelGatewaySenderHelper {

  public static ParallelGatewaySenderEventProcessor createParallelGatewaySenderEventProcessor(
      AbstractGatewaySender sender) {
    ParallelGatewaySenderEventProcessor processor = new ParallelGatewaySenderEventProcessor(sender);
    ConcurrentParallelGatewaySenderQueue queue = new ConcurrentParallelGatewaySenderQueue(sender,
        new ParallelGatewaySenderEventProcessor[] {processor});
    Set<RegionQueue> queues = new HashSet<>();
    queues.add(queue);
    when(sender.getQueues()).thenReturn(queues);
    return processor;
  }

  public static AbstractGatewaySender createGatewaySender(GemFireCacheImpl cache) {
    // Mock gateway sender
    AbstractGatewaySender sender = mock(AbstractGatewaySender.class);
    when(sender.getCache()).thenReturn(cache);
    CancelCriterion cancelCriterion = mock(CancelCriterion.class);
    when(sender.getCancelCriterion()).thenReturn(cancelCriterion);
    return sender;
  }
}
