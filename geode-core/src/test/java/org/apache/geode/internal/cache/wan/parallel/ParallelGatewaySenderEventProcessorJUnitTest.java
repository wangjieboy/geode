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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.Operation;
import org.apache.geode.internal.cache.EntryEventImpl;
import org.apache.geode.internal.cache.EnumListenerEvent;
import org.apache.geode.internal.cache.EventID;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.internal.cache.KeyInfo;
import org.apache.geode.internal.cache.LocalRegion;
import org.apache.geode.internal.cache.wan.AbstractGatewaySender;
import org.apache.geode.internal.cache.wan.AbstractGatewaySenderEventProcessor;
import org.apache.geode.internal.cache.wan.GatewaySenderEventImpl;
import org.apache.geode.internal.cache.wan.GatewaySenderStats;
import org.apache.geode.test.fake.Fakes;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class ParallelGatewaySenderEventProcessorJUnitTest {

  private GemFireCacheImpl cache;
  private AbstractGatewaySender sender;

  @Before
  public void setUpGemFire() {
    createCache();
    createGatewaySender();
  }

  private void createCache() {
    // Mock cache
    this.cache = Fakes.cache();
  }

  private void createGatewaySender() {
    // Mock gateway sender
    this.sender = ParallelGatewaySenderHelper.createGatewaySender(this.cache);
    when(this.sender.isBatchConflationEnabled()).thenReturn(true);
    when(sender.getStatistics()).thenReturn(mock(GatewaySenderStats.class));

  }

  @Test
  public void validateBatchConflationWithBatchContainingDuplicateNonConflatableEvents()
      throws Exception {
    // This is a test for GEODE-4704.
    // A batch containing events like this is conflated:
    // SenderEventImpl[id=EventIDid=57bytes;threadID=0x10018|112;sequenceID=100;bucketId=24];operation=CREATE;region=/dataStoreRegion;key=Object_13964;shadowKey=27709]
    // SenderEventImpl[id=EventIDid=57bytes;threadID=0x10018|112;sequenceID=101;bucketId=24];operation=CREATE;region=/dataStoreRegion;key=Object_14024;shadowKey=27822]
    // SenderEventImpl[id=EventIDid=57bytes;threadID=0x10018|112;sequenceID=102;bucketId=24];operation=DESTROY;region=/dataStoreRegion;key=Object_13964;shadowKey=27935]
    // SenderEventImpl[id=EventIDid=57bytes;threadID=0x10018|112;sequenceID=104;bucketId=24];operation=CREATE;region=/dataStoreRegion;key=Object_14024;shadowKey=28161]

    // Create a ParallelGatewaySenderEventProcessor
    AbstractGatewaySenderEventProcessor processor =
        ParallelGatewaySenderHelper.createParallelGatewaySenderEventProcessor(this.sender);

    // Create a batch of non-conflatable events with one duplicate
    List<GatewaySenderEventImpl> originalEvents = new ArrayList<>();
    LocalRegion lr = mock(LocalRegion.class);
    when(lr.getFullPath()).thenReturn("/dataStoreRegion");
    originalEvents.add(
        createGatewaySenderEvent(lr, Operation.CREATE, "Object_13964", "Object_13964", 100, 27709));
    originalEvents.add(
        createGatewaySenderEvent(lr, Operation.CREATE, "Object_14024", "Object_13964", 101, 27822));
    originalEvents
        .add(createGatewaySenderEvent(lr, Operation.DESTROY, "Object_13964", null, 102, 27935));
    originalEvents.add(
        createGatewaySenderEvent(lr, Operation.CREATE, "Object_14024", "Object_14024", 104, 28161));

    // Conflate the batch of events
    List<GatewaySenderEventImpl> conflatedEvents = processor.conflate(originalEvents);

    // Assert no events were conflated incorrectly
    assertThat(originalEvents).isEqualTo(conflatedEvents);
  }

  private GatewaySenderEventImpl createGatewaySenderEvent(LocalRegion lr, Operation operation,
      Object key, Object value, long sequenceId, long shadowKey) throws Exception {
    when(lr.getKeyInfo(key, value, null)).thenReturn(new KeyInfo(key, null, null));
    EntryEventImpl eei = EntryEventImpl.create(lr, operation, key, value, null, false, null);
    eei.setEventId(new EventID(new byte[16], 1l, sequenceId));
    GatewaySenderEventImpl gsei =
        new GatewaySenderEventImpl(getEnumListenerEvent(operation), eei, null);
    gsei.setShadowKey(shadowKey);
    return gsei;
  }

  private EnumListenerEvent getEnumListenerEvent(Operation operation) {
    EnumListenerEvent ele = null;
    if (operation.isCreate()) {
      ele = EnumListenerEvent.AFTER_CREATE;
    } else if (operation.isUpdate()) {
      ele = EnumListenerEvent.AFTER_UPDATE;
    } else if (operation.isDestroy()) {
      ele = EnumListenerEvent.AFTER_DESTROY;
    }
    return ele;
  }
}
