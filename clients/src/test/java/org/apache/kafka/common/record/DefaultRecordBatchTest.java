/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.record;

import org.apache.kafka.common.utils.Utils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultRecordBatchTest {

    @Test
    public void testSizeInBytes() {
        Header[] headers = new Header[] {
            new Header("foo", "value".getBytes()),
            new Header("bar", Utils.wrapNullable(null))
        };

        long timestamp = System.currentTimeMillis();
        SimpleRecord[] records = new SimpleRecord[] {
            new SimpleRecord(timestamp, "key".getBytes(), "value".getBytes()),
            new SimpleRecord(timestamp + 30000, null, "value".getBytes()),
            new SimpleRecord(timestamp + 60000, "key".getBytes(), null),
            new SimpleRecord(timestamp + 60000, "key".getBytes(), "value".getBytes(), headers)
        };
        int actualSize = MemoryRecords.withRecords(CompressionType.NONE, records).sizeInBytes();
        assertEquals(actualSize, DefaultRecordBatch.sizeInBytes(Arrays.asList(records)));
    }

    @Test
    public void testSetLastOffset() {
        SimpleRecord[] simpleRecords = new SimpleRecord[] {
            new SimpleRecord(1L, "a".getBytes(), "1".getBytes()),
            new SimpleRecord(2L, "b".getBytes(), "2".getBytes()),
            new SimpleRecord(3L, "c".getBytes(), "3".getBytes())
        };
        MemoryRecords records = MemoryRecords.withRecords(RecordBatch.MAGIC_VALUE_V2, 0L,
                CompressionType.NONE, TimestampType.CREATE_TIME, simpleRecords);

        long lastOffset = 500L;
        long firstOffset = lastOffset - simpleRecords.length + 1;

        DefaultRecordBatch batch = new DefaultRecordBatch(records.buffer());
        batch.setLastOffset(lastOffset);
        assertEquals(lastOffset, batch.lastOffset());
        assertEquals(firstOffset, batch.baseOffset());
        assertTrue(batch.isValid());

        List<RecordBatch.MutableRecordBatch> recordBatches = Utils.toList(records.batches().iterator());
        assertEquals(1, recordBatches.size());
        assertEquals(lastOffset, recordBatches.get(0).lastOffset());

        long offset = firstOffset;
        for (Record record : records.records())
            assertEquals(offset++, record.offset());
    }

    @Test
    public void testSetPartitionLeaderEpoch() {
        MemoryRecords records = MemoryRecords.withRecords(RecordBatch.MAGIC_VALUE_V2, 0L,
                CompressionType.NONE, TimestampType.CREATE_TIME,
                new SimpleRecord(1L, "a".getBytes(), "1".getBytes()),
                new SimpleRecord(2L, "b".getBytes(), "2".getBytes()),
                new SimpleRecord(3L, "c".getBytes(), "3".getBytes()));

        int leaderEpoch = 500;

        DefaultRecordBatch batch = new DefaultRecordBatch(records.buffer());
        batch.setPartitionLeaderEpoch(leaderEpoch);
        assertEquals(leaderEpoch, batch.partitionLeaderEpoch());
        assertTrue(batch.isValid());

        List<RecordBatch.MutableRecordBatch> recordBatches = Utils.toList(records.batches().iterator());
        assertEquals(1, recordBatches.size());
        assertEquals(leaderEpoch, recordBatches.get(0).partitionLeaderEpoch());
    }

    @Test
    public void testSetLogAppendTime() {
        MemoryRecords records = MemoryRecords.withRecords(RecordBatch.MAGIC_VALUE_V2, 0L,
                CompressionType.NONE, TimestampType.CREATE_TIME,
                new SimpleRecord(1L, "a".getBytes(), "1".getBytes()),
                new SimpleRecord(2L, "b".getBytes(), "2".getBytes()),
                new SimpleRecord(3L, "c".getBytes(), "3".getBytes()));

        long logAppendTime = 15L;

        DefaultRecordBatch batch = new DefaultRecordBatch(records.buffer());
        batch.setMaxTimestamp(TimestampType.LOG_APPEND_TIME, logAppendTime);
        assertEquals(TimestampType.LOG_APPEND_TIME, batch.timestampType());
        assertEquals(logAppendTime, batch.maxTimestamp());
        assertTrue(batch.isValid());

        List<RecordBatch.MutableRecordBatch> recordBatches = Utils.toList(records.batches().iterator());
        assertEquals(1, recordBatches.size());
        assertEquals(logAppendTime, recordBatches.get(0).maxTimestamp());
        assertEquals(TimestampType.LOG_APPEND_TIME, recordBatches.get(0).timestampType());

        for (Record record : records.records())
            assertEquals(logAppendTime, record.timestamp());
    }

}
