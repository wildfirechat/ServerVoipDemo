# 野火服务端音视频SDK及Demo
本项目为野火服务端音视频SDK及该SDK的演示应用。使用野火服务端音视频SDK可以使服务端跟用户之间建立双向的音视频沟通。使用此SDK可以开发AI语音助手、陪聊机器人和机器人电话服务等业务。

## 机器人角色
要使用服务端音视频SDK，必须分配一个机器人作为与用户沟通的角色。SDK需要接收IM服务的回调，包括消息回调和```conference```回调(高级版音视频需要用到这个```conference```回调)。设置机器人的消息回调地址为当前服务地址，```conference```事件会回调到```"回调地址+/conference"```。

## 支持平台
仅支持```macos + arm64```架构和```linux + x86_64```架构。其他平台架构不支持。

## 编译
打包```linux + x86_64```架构：
```
mvn -Djavacpp.platform=linux-x86_64 package
```

打包```macos + arm64```架构：
```
mvn -Djavacpp.platform=macosx-arm64 package
```

## 配置机器人
在IM服务中为当前服务创建机器人，或者使用已有机器人，配置到config目录下的```robot.properties```文件。需要修改机器人的回调地址为```http://${当前机器IP}:8083/robot/recvmsg```（这个可以根据实际情况调整，确保SDK收到消息和```conference``` 事件。

如果使用免费版本音视频，需要部署turn服务，并配置到```application.properties```文件中，注意上线前一定要切换到你们自己的turn服务。如果是音视频高级版，可以不用配置turn服务。

## 运行
在```target```目录找到```server_voip_demo-XXXX.jar```，把jar包和放置配置文件的```config```目录放到一起，然后执行下面命令：
```
java -jar server_voip_demo-XXXXX.jar
```

## 测试
使用客户端给机器人打音视频通话，等待3秒钟后，服务就会接听。语音会延迟3秒播放收到的语音。如果是视频电话会播放配置指定的视频文件，并把收到的视频每隔15秒保存一张图片。

也可以给机器人发送文本消息 ```给我打电话``` ，服务会立即给你打个视频电话。

## 音视频版本
SDK同时支持免费版音视频和音视频高级版。当收到来电时，SDK根据来电消息内容，可以知道是那种类型的音视频，自动选择对应版本。当播出时，需要指定是高级版音视频还是免费版本音视频。

在这个demo中会记录收到的音视频通话中音视频SDK类型，这样当收到 ```给我打电话``` ，服务会使用合适的版本发给用户。

免费版本音视频只支持1对1通话，可以在单聊和群聊中给一个用户进行通话。高级版音视频支持多人通话。

## 移植到其他项目中
SDK在[src/lib](src/lib)目录下，需要拷贝到项目中，另外参考本demo的[pom.xml](./pom.xml)把所需要的依赖添加过去。再处理接收消息和```conference```事件回调传入到SDK中。使用方法请参考本demo中的接听和发起通话的示例代码。

#### LICENSE
UNDER MIT LICENSE. 详情见LICENSE文件
