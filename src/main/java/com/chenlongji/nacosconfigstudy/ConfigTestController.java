package com.chenlongji.nacosconfigstudy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author clj
 */
@RestController
@RefreshScope
public class ConfigTestController {
    @Value("${user.name:admin}")
    private String name;

    @Value("${user.age:18}")
    private Integer age;

    @Value("${user.desc:Controller默认值}")
    private String desc;



    // 接口访问地址: http://localhost:8118/getNacosConfig
    @GetMapping("/getNacosConfig")
    public String getNacosConfig (){
        return "nacosConfig: name=" + name + ", age=" + age + ", desc=" + desc;
    }

    /**
     * dataId说明:
     *  指定前缀时: ${spring.cloud.nacos.config.prefix}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
     *  没指定前缀时:        ${spring.application.name}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
     *  值说明:
     *      spring.cloud.nacos.config.prefix为指定的前缀, 不指定则使用spring.application.name
     *      spring.application.name为应用名.
     *      spring.profiles.active为激活的环境.
     *          没有配置spring.profile.active时, dataId=${spring.application.name}.${spring.cloud.nacos.config.file-extension}.
     *      file-extension为配置文件拓展名. 默认值为properties, 配置路径spring.cloud.nacos.config.file-extension.
     *          注意: springboot项目配置该值时需在bootstrap.yml或在bootstrap.properties配置才有效.
     *      旧版本时: ${spring.application.name}前还可以添加${spring.application.group}:
     *          即${spring.application.group}:${spring.application.name}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
     * nacos读取多个配置:
     *  https://blog.csdn.net/a745233700/article/details/122916208
     *  未亲测
     */



/*    @Autowired
    private NotifyUtil notifyUtil;

    // 接口访问地址: http://localhost:8118/getApiNacosConfig
    @GetMapping("/getApiNacosConfig")
    public String getApiNacosConfig (){
        // 测试添加api
        return "notifyUtilInfo: " + notifyUtil.getInfo();
    }*/

}
