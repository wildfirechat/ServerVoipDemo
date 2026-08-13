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

    @PostMapping(value = "/robot/recvmsg/{robotId}", produces = "application/json;charset=UTF-8"   )
    public Object recvMsg(@PathVariable String robotId, @RequestBody OutputMessageData messageData) {
        if(!mCallService.hasRobot(robotId)) {
            return "unknown robot: " + robotId;
        }
        mService.onReceiveMessage(robotId, messageData);
        return "ok";
    }

    @PostMapping(value = "/robot/recvmsg/{robotId}/conference", produces = "application/json;charset=UTF-8"   )
    public Object recvConferenceEvent(@PathVariable String robotId, @RequestBody String event) {
        if(!mCallService.hasRobot(robotId)) {
            return "unknown robot: " + robotId;
        }
        mService.onReceiveConferenceEvent(robotId, event);
        return "ok";
    }
}
