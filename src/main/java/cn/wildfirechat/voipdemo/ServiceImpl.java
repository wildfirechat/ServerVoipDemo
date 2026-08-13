package cn.wildfirechat.voipdemo;

import cn.wildfirechat.voipdemo.call.CallService;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.RobotService;
import cn.wildfirechat.sdk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@org.springframework.stereotype.Service
public class ServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

    @Autowired
    private CallService callService;

    public void onReceiveMessage(String robotId, OutputMessageData messageData) {
        LOG.info("robot {} on receive message {}", robotId, messageData.getMessageId());

        if(messageData.getPayload().getType() >= 400 && messageData.getPayload().getType() <= 420) {
            callService.onReceiveCallMessage(robotId, messageData);
            return;
        }

        if(messageData.getPayload().getType() == 1
                && ("给我打电话".equals(messageData.getPayload().getSearchableContent()) || "给我打个电话".equals(messageData.getPayload().getSearchableContent()) || "call me".equalsIgnoreCase(messageData.getPayload().getSearchableContent()))
                && (messageData.getConv().getType() == ProtoConstants.ConversationType.ConversationType_Private || messageData.getConv().getType() == ProtoConstants.ConversationType.ConversationType_Group)) {
            if(messageData.getConv().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                //单聊的target为对方id，收到的消息需要设置一下target。
                messageData.getConv().setTarget(messageData.getSender());
            }

            if(callService.hasPreferEngine(robotId, messageData.getSender())) {
                if(messageData.getConv().getType() == ProtoConstants.ConversationType.ConversationType_Private) {
                    callService.startPrivateCall(robotId, messageData.getConv(), false, callService.isAdvanceEngine(robotId, messageData.getSender()));
                } else {
                    callService.startGroupCall(robotId, messageData.getConv(), Arrays.asList(messageData.getSender()), false, callService.isAdvanceEngine(robotId, messageData.getSender()));
                }
            } else {
                RobotService robotService = callService.getRobotService(robotId);
                if(robotService == null) {
                    LOG.error("robot {} not exist!", robotId);
                    return;
                }
                MessagePayload payload = new MessagePayload();
                payload.setType(1);
                payload.setSearchableContent("请先给我打个电话，以后我才能给您电话");
                try {
                    IMResult<SendMessageResult> result = robotService.sendMessage(robotId, messageData.getConv(), payload);
                    if (result != null) {
                        if (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                            LOG.info("Send response success");
                        } else {
                            LOG.error("Send response error {}", result.getCode());
                        }
                    } else {
                        LOG.error("Send response is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("Send response execption");
                }
            }
            return;
        }
        return;
    }

    public void onReceiveConferenceEvent(String event) {
        callService.onConferenceEvent(event);
    }

}
