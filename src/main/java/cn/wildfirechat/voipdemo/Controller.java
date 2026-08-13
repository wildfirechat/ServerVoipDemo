package cn.wildfirechat.voipdemo;


import cn.wildfirechat.pojos.OutputMessageData;
import cn.wildfirechat.voipdemo.call.CallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {
    @Autowired
    private ServiceImpl mService;

    @Autowired
    private CallService mCallService;

    @PostMapping(value = "/robot/recvmsg", produces = "application/json;charset=UTF-8"   )
    public Object recvMsg(@RequestBody OutputMessageData messageData) {
        if(!mCallService.hasRobot(messageData.getToRobotId())) {
            return "unknown robot: " + messageData.getToRobotId();
        }
        mService.onReceiveMessage(messageData.getToRobotId(), messageData);
        return "ok";
    }

    @PostMapping(value = "/robot/recvmsg/conference", produces = "application/json;charset=UTF-8"   )
    public Object recvConferenceEvent(@RequestBody String event) {
        mService.onReceiveConferenceEvent(event);
        return "ok";
    }
}
