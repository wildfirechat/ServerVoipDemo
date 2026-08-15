# 野火服务端音视频SDK及Demo
本项目为野火服务端音视频SDK及该SDK的演示应用，实现了服务和普通用户之间进行双向音视频通话功能。使用此SDK可以开发AI语音助手、陪聊机器人和机器人电话服务等业务。

## 机器人角色
要使用服务端音视频SDK，必须分配一个机器人作为与用户沟通的角色。在用户看来是在跟一个机器人通话。一个机器人角色可以同时加入多路音视频通话。一个服务也支持同时配置多个机器人（在config目录下的```robot.properties```中配置），每个机器人独立处理自己的来电和通话。

## 支持平台
仅支持```macos + arm64```、```linux + x86_64```和```linux + aarch64```架构。其他平台架构不支持。

linux 系统对 glibc 版本有要求：```linux + x86_64```和```linux + aarch64```均要求 **glibc 2.29 及以上**。在目标机器上执行```ldd --version```可以查看 glibc 版本。

常见发行版的 glibc 版本及支持情况：

| 发行版 | glibc 版本 | 是否支持 |
| --- | --- | --- |
| Ubuntu 20.04 / 22.04 / 24.04 | 2.31 / 2.35 / 2.39 | 支持 |
| Ubuntu 18.04 | 2.27 | 不支持 |
| Debian 11 (bullseye) / 12 (bookworm) | 2.31 / 2.36 | 支持 |
| Debian 10 (buster) | 2.28 | 不支持 |
| RHEL 9 / CentOS Stream 9 | 2.34 | 支持 |
| CentOS 8 / RHEL 8 / CentOS Stream 8 | 2.28 | 不支持 |
| CentOS 7 / RHEL 7 | 2.17 | 不支持 |
| openEuler 22.03 / 24.03 | 2.34 / 2.38 | 支持 |
| openEuler 20.03 | 2.28 | 不支持 |
| 统信 UOS V20 | 2.28~2.31（因版本和更新级别而异） | 以实际为准 |
| 银河麒麟 V10 | 2.28 | 不支持 |
| Alpine Linux | musl（非 glibc） | 不支持 |

## 编译
### 环境准备
需要 JDK 8 或更高版本（项目按 Java 8 语法编译，高版本 JDK 编译和运行均可）。安装 JDK 后把```JAVA_HOME```环境变量指向 JDK 安装目录，并把```$JAVA_HOME/bin```加入```PATH```。执行```java -version```和```mvn -v```能正确显示版本号即配置完成。

### 打包
项目依赖两类平台原生库：javacv/ffmpeg（用```javacpp.platform```选择）和 webrtc-java（用```webrtc.platform```选择，默认为```linux-x86_64```）。打包时这两个参数必须与目标运行平台一致，否则无法运行。

打包```linux + x86_64```架构（```webrtc.platform```默认即为此架构）：
```
mvn -Djavacpp.platform=linux-x86_64 package
```

打包```linux + aarch64```架构：
```
mvn -Djavacpp.platform=linux-arm64 -Dwebrtc.platform=linux-aarch64 package
```

打包```macos + arm64```架构：
```
mvn -Djavacpp.platform=macosx-arm64 -Dwebrtc.platform=macos-aarch64 package
```

## 配置机器人
在IM服务中为当前服务创建机器人，或者使用已有机器人，支持配置多个机器人。每个机器人的回调地址都配置为```http://${当前机器IP}:8883/robot/recvmsg```（会议事件回调为```http://${当前机器IP}:8883/robot/recvmsg/conference```），服务会根据消息中的toRobotId自动分发到对应的机器人。把所有机器人的信息配置到本项目config目录下的```robot.properties```文件里。

如果使用免费版本音视频，需要部署turn服务，并配置到config目录下的```application.properties```文件中，注意上线前一定要切换到你们自己的turn服务。如果是音视频高级版，可以不用配置turn服务。

如果需要机器人只发送音视频而不接收对方的音视频流（仅对高级版音视频有效），把config目录下```application.properties```文件中的```call.send.only```设置为```true```，默认为```false```双向收发。

视频通话中收到的对端视频默认每隔15秒截屏保存一张bmp图片到运行目录（调试用），长时间运行会积累大量文件占用磁盘，上线时建议把```application.properties```中的```video.snapshot.enabled```设置为```false```关闭。

服务监听端口在```application.properties```的```server.port```中配置（默认8883），机器人回调地址中的端口要与此一致。

## 运行
在```target```目录找到```server_voip_demo-XXXX.jar```，把jar包和放置配置文件的```config```目录放到一起，然后执行下面命令：
```
java -jar server_voip_demo-XXXXX.jar
```

## 测试
使用客户端给机器人打音视频通话，等待3秒钟后，服务就会接听。语音会延迟3秒把收到的语音回播给对方。如果是视频电话，会把```video.file.path```配置的视频文件循环播放给对方，并把收到的视频每隔15秒保存一张bmp图片到运行目录。

也可以给机器人发送文本消息 ```给我打电话```、```给我打个电话``` 或 ```call me```，服务会立即给你打个视频电话。

## 注意事项
- 打包时的平台参数（```javacpp.platform```和```webrtc.platform```）必须与实际运行平台一致，在 macOS 上打的包不能直接在 Linux 上运行，反之亦然。
- 运行目录下必须有```config```目录（与jar包同级），否则启动时会因读不到配置而失败。
- 确保IM服务能访问到本服务的回调地址，防火墙/安全组需要放行服务端口（默认8883）。
- 示例配置中的turn服务仅供测试，上线前务必替换为自己的turn服务，并确认地址、用户名、密码正确。

## 音视频版本
SDK同时支持免费版音视频和音视频高级版。当收到来电时，SDK根据来电消息内容，可以知道是哪种类型的音视频，自动选择对应版本。当呼出时，需要指定是高级版音视频还是免费版本音视频。

在这个demo中会记录收到的音视频通话中音视频SDK类型，这样当收到 ```给我打电话``` ，服务会使用合适的版本发给用户。

免费版本音视频只支持1对1通话，可以在单聊和群聊中给一个用户进行通话。高级版音视频支持多人通话。

## 接入音视频
### 接入音频
重写代码中的[EchoAudioDevice](./src/main/java/cn/wildfirechat/voipdemo/call/EchoAudioDevice.java)类。这个类中有音频设备的捕获和播放的生命周期接口，还有播放和抓取音频数据的接口。在这些接口中实现业务，比如对接AI语音等。

### 捕获视频和收取视频
重写代码中的[FileVideoCapture](./src/main/java/cn/wildfirechat/voipdemo/call/FileVideoCapture.java)类。把要输出的视频流在```onFetchFrame```方法中拷贝到对应的buffer中。

重写代码中的[ImageVideoSink](./src/main/java/cn/wildfirechat/voipdemo/call/ImageVideoSink.java)类。在```onVideoFrame```方法中会收到一帧一帧的Frame。

## 移植到其他项目中
SDK在[src/lib](src/lib)目录下，需要拷贝到项目中，另外参考本demo的[pom.xml](./pom.xml)把所需要的依赖添加过去。再处理接收消息和```conference```事件回调传入到SDK中。使用方法请参考本demo中的接听和发起通话的示例代码。

#### LICENSE
UNDER MIT LICENSE. 详情见LICENSE文件
