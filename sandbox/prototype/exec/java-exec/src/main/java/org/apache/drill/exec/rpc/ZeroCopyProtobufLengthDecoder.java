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
package org.apache.drill.exec.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import com.google.protobuf.CodedInputStream;

/**
 * Modified version of ProtobufVarint32FrameDecoder that avoids bytebuf copy.
 */
public class ZeroCopyProtobufLengthDecoder extends ByteToMessageDecoder {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ZeroCopyProtobufLengthDecoder.class);

  
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, MessageBuf<Object> out) throws Exception {

    if(!ctx.channel().isOpen()){
      if(in.readableBytes() > 0) logger.info("Channel is closed, discarding remaining {} byte(s) in buffer.", in.readableBytes());
      in.skipBytes(in.readableBytes());
      return;
    }
    
    in.markReaderIndex();
    final byte[] buf = new byte[5];
    for (int i = 0; i < buf.length; i ++) {
        if (!in.isReadable()) {
            in.resetReaderIndex();
            return;
        }

        buf[i] = in.readByte();
        if (buf[i] >= 0) {
          
            int length = CodedInputStream.newInstance(buf, 0, i + 1).readRawVarint32();
            
            if (length < 0) {
                throw new CorruptedFrameException("negative length: " + length);
            }
            if (length == 0){
                throw new CorruptedFrameException("Received a message of length 0.");
            }

            if (in.readableBytes() < length) {
                in.resetReaderIndex();
                return;
            } else {
                ByteBuf outBuf = in.slice(in.readerIndex(), length);
                in.retain();
                in.skipBytes(length);
                if(RpcConstants.EXTRA_DEBUGGING) logger.debug(String.format("ReaderIndex is %d after length header of %d bytes and frame body of length %d bytes.", in.readerIndex(), i+1, length));
                out.add(outBuf);
                return;
            }
        }
    }

    // Couldn't find the byte whose MSB is off.
    throw new CorruptedFrameException("length wider than 32-bit");
    
  }


}
