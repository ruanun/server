/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;


import cn.wildfirechat.common.APIPath;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.SendMessageData;
import cn.wildfirechat.pojos.SendMessageResult;
import io.moquette.persistence.ServerAPIHelper;
import io.moquette.persistence.TargetEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.concurrent.Executor;

@Route(APIPath.Msg_Send)
@HttpMethod("POST")
public class SendMessageAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            SendMessageData sendMessageData = getRequestBody(request.getNettyRequest(), SendMessageData.class);
            if (SendMessageData.isValide(sendMessageData) && !StringUtil.isNullOrEmpty(sendMessageData.getSender())) {
                sendApiMessage(sendMessageData.getSender(), IMTopic.SendMessageTopic, sendMessageData.toProtoMessage().toByteArray(), result -> {
                    ByteBuf byteBuf = Unpooled.buffer();
                    byteBuf.writeBytes(result);
                    ErrorCode errorCode = ErrorCode.fromCode(byteBuf.readByte());
                    if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
                        long messageId = byteBuf.readLong();
                        long timestamp = byteBuf.readLong();
                        sendResponse(response, null, new SendMessageResult(messageId, timestamp));
                    } else {
                        sendResponse(response, errorCode, null);
                    }
                });
                return false;
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }
        }
        return true;
    }
}
