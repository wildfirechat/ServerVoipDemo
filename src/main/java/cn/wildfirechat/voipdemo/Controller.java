package cn.wildfirechat.voipdemo;


import cn.wildfirechat.pojos.OutputMessageData;
import cn.wildfirechat.voipdemo.call.CallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {
    private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

    @Autowired
    private ServiceImpl mService;

    @Autowired
    private CallService mCallService;

    @PostMapping(value = "/robot/recvmsg", produces = "application/json;charset=UTF-8"   )
    public Object recvMsg(@RequestBody OutputMessageData messageData) {
        LOG.debug("recvMsg entry, toRobotId={}, messageId={}, type={}, sender={}",
                messageData.getToRobotId(),
                messageData.getMessageId(),
                messageData.getPayload() != null ? messageData.getPayload().getType() : -1,
                messageData.getSender());

        if(!mCallService.hasRobot(messageData.getToRobotId())) {
            LOG.warn("recvMsg unknown robot: {}", messageData.getToRobotId());
            return "unknown robot: " + messageData.getToRobotId();
        }

        try {
            mService.onReceiveMessage(messageData.getToRobotId(), messageData);
            LOG.debug("recvMsg done, toRobotId={}, messageId={}", messageData.getToRobotId(), messageData.getMessageId());
            return "ok";
        } catch (Exception e) {
            LOG.error("recvMsg exception, toRobotId={}, messageId={}", messageData.getToRobotId(), messageData.getMessageId(), e);
            throw e;
        }
    }

    @PostMapping(value = "/robot/recvmsg/conference", produces = "application/json;charset=UTF-8"   )
    public Object recvConferenceEvent(@RequestBody String event) {
        LOG.debug("recvConferenceEvent entry, event={}", event);
        try {
            mService.onReceiveConferenceEvent(event);
            LOG.debug("recvConferenceEvent done");
            return "ok";
        } catch (Exception e) {
            LOG.error("recvConferenceEvent exception", e);
            throw e;
        }
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
        LOG.error("Controller unhandled exception", e);
        return "error";
    }
}
