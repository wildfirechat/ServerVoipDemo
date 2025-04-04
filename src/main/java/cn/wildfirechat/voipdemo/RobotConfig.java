package cn.wildfirechat.voipdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="robot")
@PropertySource(value = "file:config/robot.properties", encoding = "UTF-8")
public class RobotConfig {
    public String im_url;
    public String im_secret;
    public String im_id;

    public String getIm_id() {
        return im_id;
    }

    public void setIm_id(String im_id) {
        this.im_id = im_id;
    }

    public String getIm_url() {
        return im_url;
    }

    public void setIm_url(String im_url) {
        this.im_url = im_url;
    }

    public String getIm_secret() {
        return im_secret;
    }

    public void setIm_secret(String im_secret) {
        this.im_secret = im_secret;
    }
}
