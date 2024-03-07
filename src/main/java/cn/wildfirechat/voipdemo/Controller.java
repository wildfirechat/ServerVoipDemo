package cn.wildfirechat.voipdemo;


import cn.wildfirechat.pojos.OutputMessageData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {
    @Autowired
    private ServiceImpl mService;

    @PostMapping(value = "/robot/recvmsg", produces = "application/json;charset=UTF-8"   )
    public Object recvMsg(@RequestBody OutputMessageData messageData) {
        mService.onReceiveMessage(messageData);
        return "ok";
    }

    @PostMapping(value = "/robot/recvmsg/conference", produces = "application/json;charset=UTF-8"   )
    public Object recvConferenceEvent(@RequestBody String event) {
        mService.onReceiveConferenceEvent(event);
        return "ok";
    }
}
