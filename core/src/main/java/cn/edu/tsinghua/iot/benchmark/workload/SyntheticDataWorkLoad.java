/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package cn.edu.tsinghua.iot.benchmark.workload;

import cn.edu.tsinghua.iot.benchmark.entity.Batch.Batch;
import cn.edu.tsinghua.iot.benchmark.entity.Sensor;
import cn.edu.tsinghua.iot.benchmark.exception.WorkloadException;
import cn.edu.tsinghua.iot.benchmark.schema.schemaImpl.DeviceSchema;

import java.util.*;

public class SyntheticDataWorkLoad extends GenerateDataWorkLoad {

  private final Map<DeviceSchema, Long> maxTimestampIndexMap;
  private long insertLoop = 0;
  private int deviceIndex = 0;
  private int sensorIndex = 0;

  public SyntheticDataWorkLoad(List<DeviceSchema> deviceSchemas) {
    this.deviceSchemas = deviceSchemas;
    maxTimestampIndexMap = new HashMap<>();
    for (DeviceSchema schema : deviceSchemas) {
      if (config.isIS_SENSOR_TS_ALIGNMENT()) {
        maxTimestampIndexMap.put(schema, 0L);
      } else {
        for (Sensor sensor : schema.getSensors()) {
          DeviceSchema deviceSchema =
              new DeviceSchema(
                  schema.getDeviceId(), Collections.singletonList(sensor), config.getDEVICE_TAGS());
          maxTimestampIndexMap.put(deviceSchema, 0L);
        }
      }
    }
    this.deviceSchemaSize = deviceSchemas.size();
  }

  @Override
  public Batch getOneBatch() throws WorkloadException {
    Batch batch = new Batch();
    // create the schema of batch
    DeviceSchema deviceSchema =
        new DeviceSchema(
            deviceSchemas.get(deviceIndex).getDeviceId(),
            deviceSchemas.get(deviceIndex).getSensors(),
            deviceSchemas.get(deviceIndex).getTags());
    if (!config.isIS_SENSOR_TS_ALIGNMENT()) {
      List<Sensor> sensors = new ArrayList<>();
      sensors.add(deviceSchema.getSensors().get(sensorIndex));
      deviceSchema.setSensors(sensors);
      batch.setColIndex(sensorIndex);
    }
    batch.setDeviceSchema(deviceSchema);
    // create the data of batch
    long rowOffset = insertLoop * config.getBATCH_SIZE_PER_WRITE();
    for (long offset = 0; offset < config.getBATCH_SIZE_PER_WRITE(); offset++, rowOffset++) {
      addOneRowIntoBatch(batch, rowOffset);
    }

    // move
    if (config.isIS_SENSOR_TS_ALIGNMENT()) {
      deviceIndex++;
    } else {
      sensorIndex++;
      if (sensorIndex >= deviceSchemas.get(deviceIndex).getSensors().size()) {
        deviceIndex++;
        sensorIndex = 0;
      }
    }
    if (deviceIndex >= deviceSchemaSize) {
      deviceIndex = 0;
      insertLoop++;
    }
    return batch;
  }
}
