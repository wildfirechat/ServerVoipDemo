package cn.wildfirechat.voipdemo.call;

import cn.wildfirechat.*;
import cn.wildfirechat.impl.SignalServerImpl;
import cn.wildfirechat.voipdemo.RobotConfig;
import cn.wildfirechat.pojos.Conversation;
import cn.wildfirechat.pojos.OutputMessageData;
import cn.wildfirechat.sdk.RobotService;
import dev.onvoid.webrtc.media.video.VideoTrack;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Service
public class CallService {
    private static final Logger LOG = LoggerFactory.getLogger(CallService.class);
    @Autowired
    private RobotConfig mRobotConfig;

    @Value("${ice.url}")
    private String iceUrl;

    @Value("${ice.password}")
    private String icePassword;

    @Value("${ice.username}")
    private String iceUsername;

    @Value("${video.file.path}")
    private String videoFilePath;

    //是否只发送音视频，不接收对方的音视频流（仅对高级版音视频有效）
    @Value("${call.send.only:false}")
    private boolean sendOnly;

    //每个机器人一套独立的上下文：RobotService + AVEngineKit 实例
    private final Map<String, RobotContext> robotContextMap = new ConcurrentHashMap<>();

    // ConcurrentHashMap: entries are removed on call end, otherwise the map would grow forever
    private Map<String, ImageVideoSink> imageVideoSinkMap = new ConcurrentHashMap<>();

    //key为 robotId + "_" + userId
    private final Map<String, Boolean> engineTypeMap = new ConcurrentHashMap<>();

    private static class RobotContext {
        RobotService robotService;
        AVEngineKit engine;
    }

    @PostConstruct
    private void init() {
        for (RobotConfig.RobotInfo robotInfo : mRobotConfig.getList()) {
            initRobot(robotInfo);
        }

        //设置turn服务地址，如果有多个，可以调用多次。如果是高级版，可以不用设置turn服务。全局生效，只需设置一次
        if(!StringUtils.isEmpty(iceUrl)) {
            //如果是高级版，不用设置turn服务。
            AVEngineKit.addIceServer(iceUrl, iceUsername, icePassword);
        }

        //打开webrtc的日志，一般不用打开，除非出现问题需要debug
        //AVEngineKit.enableWebRTCLog();
    }

    private void initRobot(RobotConfig.RobotInfo robotInfo) {
        String robotId = robotInfo.getIm_id();
        RobotService robotService = new RobotService(robotInfo.getIm_url(), robotId, robotInfo.getIm_secret());

        AVEngineKit engine = new AVEngineKit();
        //初始化音视频SDK
        engine.init(robotId, new SignalServerImpl(robotService), callSession -> {
            for (String participant : callSession.getParticipants()) {
                engineTypeMap.put(engineTypeKey(robotId, participant), callSession.isAdvanceEngine());
            }

            callSession.setEventCallback(new CallEventCallback() {
                @Override
                public void onCallStateUpdated(CallSession callSession, CallState state) {

                }

                @Override
                public void onParticipantJoined(CallSession callSession, String userId) {

                }

                @Override
                public void onParticipantConnected(CallSession callSession, String userId) {

                }

                @Override
                public void onReceiveRemoteVideoTrack(CallSession callSession, String userId, VideoTrack videoTrack) {
                    String key = userId + "_" + callSession.getCallId();
                    ImageVideoSink imageVideoSink = new ImageVideoSink(userId, callSession.getCallId());
                    ImageVideoSink existing = imageVideoSinkMap.putIfAbsent(key, imageVideoSink);
                    if(existing == null) {
                        videoTrack.addSink(imageVideoSink);
                    } else {
                        // Not used; let its worker thread exit instead of leaking it
                        imageVideoSink.onCallEnded();
                    }
                }

                @Override
                public void onParticipantLeft(CallSession callSession, String userId, CallEndReason reason) {

                }

                @Override
                public void onCallEnd(CallSession callSession, CallEndReason endReason) {
                    String callId = callSession.getCallId();
                    // Stop and remove this call's sinks so the map doesn't grow across calls
                    imageVideoSinkMap.values().removeIf(value -> {
                        if(value.callId.equals(callId)) {
                            value.onCallEnded();
                            return true;
                        }
                        return false;
                    });
                }
            });

            callSession.setAudioDevice(new EchoAudioDevice(callSession.getConversation()));
            if(!callSession.isAudioOnly()) {
                callSession.setVideoCapture(new FileVideoCapture(videoFilePath, callSession.getConversation(), callSession.getCallId()));
            }

            //延迟3秒接听
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    callSession.answer(callSession.isAudioOnly(), sendOnly);
                }
            }).start();
        });

        RobotContext context = new RobotContext();
        context.robotService = robotService;
        context.engine = engine;
        robotContextMap.put(robotId, context);
        LOG.info("robot {} initialized", robotId);
    }

    private static String engineTypeKey(String robotId, String userId) {
        return robotId + "_" + userId;
    }

    public boolean hasRobot(String robotId) {
        return robotContextMap.containsKey(robotId);
    }

    public RobotService getRobotService(String robotId) {
        RobotContext context = robotContextMap.get(robotId);
        return context != null ? context.robotService : null;
    }

    private AVEngineKit getEngine(String robotId) {
        RobotContext context = robotContextMap.get(robotId);
        if(context == null) {
            LOG.error("robot {} not exist!", robotId);
            return null;
        }
        return context.engine;
    }

    public boolean hasPreferEngine(String robotId, String userId) {
        return engineTypeMap.containsKey(engineTypeKey(robotId, userId));
    }

    public boolean isAdvanceEngine(String robotId, String userId) {
        return engineTypeMap.get(engineTypeKey(robotId, userId));
    }

    public void startPrivateCall(String robotId, Conversation conversation, boolean audioOnly, boolean advanceEngine) {
        AVEngineKit engine = getEngine(robotId);
        if(engine == null) {
            return;
        }
        CallSession callSession = engine.startPrivateCall(conversation, audioOnly, advanceEngine, sendOnly, new EchoAudioDevice(conversation), new CallEventCallback() {
            @Override
            public void onCallStateUpdated(CallSession callSession, CallState state) {

            }

            @Override
            public void onParticipantJoined(CallSession callSession, String userId) {

            }

            @Override
            public void onParticipantConnected(CallSession callSession, String userId) {

            }

            @Override
            public void onReceiveRemoteVideoTrack(CallSession callSession, String userId, VideoTrack videoTrack) {

            }

            @Override
            public void onParticipantLeft(CallSession callSession, String userId, CallEndReason reason) {

            }

            @Override
            public void onCallEnd(CallSession callSession, CallEndReason endReason) {

            }
        }, 0, null);
        if(!callSession.isAudioOnly()) {
            callSession.setVideoCapture(new FileVideoCapture(videoFilePath, callSession.getConversation(), callSession.getCallId()));
        }
    }

    public void startGroupCall(String robotId, Conversation conversation, List<String> targets, boolean audioOnly, boolean advanceEngine) {
        AVEngineKit engine = getEngine(robotId);
        if(engine == null) {
            return;
        }
        CallSession callSession = engine.startGroupCall(conversation, targets, audioOnly, advanceEngine, sendOnly, new EchoAudioDevice(conversation), new CallEventCallback() {
            @Override
            public void onCallStateUpdated(CallSession callSession, CallState state) {

            }

            @Override
            public void onParticipantJoined(CallSession callSession, String userId) {

            }

            @Override
            public void onParticipantConnected(CallSession callSession, String userId) {

            }

            @Override
            public void onReceiveRemoteVideoTrack(CallSession callSession, String userId, VideoTrack videoTrack) {

            }

            @Override
            public void onParticipantLeft(CallSession callSession, String userId, CallEndReason reason) {

            }

            @Override
            public void onCallEnd(CallSession callSession, CallEndReason endReason) {

            }
        }, 0, null);
        if(!callSession.isAudioOnly()) {
            callSession.setVideoCapture(new FileVideoCapture(videoFilePath, callSession.getConversation(), callSession.getCallId()));
        }
    }

    public void onConferenceEvent(String event) {
        for (RobotContext engineContext : robotContextMap.values()) {
            if(engineContext.engine.onConferenceEvent(event)) {
                break;
            }
        }
    }

    public boolean onReceiveCallMessage(String robotId, OutputMessageData messageData) {
        AVEngineKit engine = getEngine(robotId);
        if(engine == null) {
            return false;
        }
        if(messageData.getPayload().getType() == 408) { //会议邀请
            try {
                String callId = messageData.getPayload().getContent();
                JSONObject jsonObject = (JSONObject)(new JSONParser()).parse(new String(Base64.getDecoder().decode(messageData.getPayload().getBase64edData())));

                String host = (String)jsonObject.get("h");
                String title = (String)jsonObject.get("t");
                String pin = (String)jsonObject.get("p");
                boolean audience = false;
                if(jsonObject.get("audience") instanceof Long) {
                    audience = (Long)jsonObject.get("audience") > 0;
                } else if(jsonObject.get("audience") instanceof Integer) {
                    audience = (Integer)jsonObject.get("audience") > 0;
                }

                boolean advanced = false;
                if(jsonObject.get("advanced") instanceof Long) {
                    advanced = (Long)jsonObject.get("advanced") > 0;
                } else if(jsonObject.get("advanced") instanceof Integer) {
                    advanced = (Integer)jsonObject.get("advanced") > 0;
                }

                CallSession callSession = engine.joinConference(callId, pin, audience, false, sendOnly, new EchoAudioDevice(null), new CallEventCallback() {
                    @Override
                    public void onCallStateUpdated(CallSession callSession, CallState state) {

                    }

                    @Override
                    public void onParticipantJoined(CallSession callSession, String userId) {

                    }

                    @Override
                    public void onParticipantConnected(CallSession callSession, String userId) {

                    }

                    @Override
                    public void onReceiveRemoteVideoTrack(CallSession callSession, String userId, VideoTrack videoTrack) {
                        String key = userId + "_" + callSession.getCallId();
                        ImageVideoSink imageVideoSink = new ImageVideoSink(userId, callSession.getCallId());
                        ImageVideoSink existing = imageVideoSinkMap.putIfAbsent(key, imageVideoSink);
                        if(existing == null) {
                            videoTrack.addSink(imageVideoSink);
                        } else {
                            // Not used; let its worker thread exit instead of leaking it
                            imageVideoSink.onCallEnded();
                        }
                    }

                    @Override
                    public void onParticipantLeft(CallSession callSession, String userId, CallEndReason reason) {

                    }

                    @Override
                    public void onCallEnd(CallSession callSession, CallEndReason endReason) {
                        String callId = callSession.getCallId();
                        // Stop and remove this call's sinks so the map doesn't grow across calls
                        imageVideoSinkMap.values().removeIf(value -> {
                            if(value.callId.equals(callId)) {
                                value.onCallEnded();
                                return true;
                            }
                            return false;
                        });
                    }
                }, 0, null);

                callSession.setVideoCapture(new FileVideoCapture(videoFilePath, callSession.getConversation(), callSession.getCallId()));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return engine.onReceiveCallMessage(messageData);
    }
}
