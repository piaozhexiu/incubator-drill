/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.drill.exec.rpc.bit;

import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.RpcCheckedFuture;
import org.apache.drill.exec.rpc.RpcConnectionHandler;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.MessageLite;

public abstract class ListeningBitCommand<T extends MessageLite> implements BitCommand<T> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ListeningBitCommand.class);

  private final RpcOutcomeListener<T> listener;

  public ListeningBitCommand(RpcOutcomeListener<T> listener) {
    this.listener = listener;
  }

  public abstract void doRpcCall(RpcOutcomeListener<T> outcomeListener, BitConnection connection);

  @Override
  public void connectionAvailable(BitConnection connection) {
    
    doRpcCall(new DeferredRpcOutcome(), connection);
  }

  @Override
  public void connectionSucceeded(BitConnection connection) {
    connectionAvailable(connection);
  }

  private class DeferredRpcOutcome implements RpcOutcomeListener<T> {

    @Override
    public void failed(RpcException ex) {
      listener.failed(ex);
    }

    @Override
    public void success(T value) {
      listener.success(value);
    }

  }


  @Override
  public void connectionFailed(FailureType type, Throwable t) {
    listener.failed(RpcException.mapException(
        String.format("Command failed while establishing connection.  Failure type %s.", type), t));
  }

}