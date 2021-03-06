/*
 * Copyright 2018-present HiveMQ and the HiveMQ Community
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

package com.hivemq.client.mqtt.mqtt5.message;

import com.hivemq.client.annotations.DoNotImplement;

/**
 * Reason Code according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 * @since 1.0
 */
@DoNotImplement
public interface Mqtt5ReasonCode {

    /**
     * @return the byte code of this Reason Code.
     */
    int getCode();

    /**
     * @return whether this Reason Code is an Error Code.
     */
    default boolean isError() {
        return getCode() >= 0x80;
    }

    /**
     * @return whether this Reason Code can be sent by the server according to the MQTT 5 specification.
     */
    default boolean canBeSentByServer() {
        return true;
    }

    /**
     * @return whether this Reason Code can be sent by the client according to the MQTT 5 specification.
     */
    default boolean canBeSentByClient() {
        return false;
    }

    /**
     * @return whether this Reason Code can be set by the user.
     */
    default boolean canBeSetByUser() {
        return false;
    }
}
