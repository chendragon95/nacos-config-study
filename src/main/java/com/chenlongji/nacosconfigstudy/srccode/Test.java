package com.chenlongji.nacosconfigstudy.srccode;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author clj
 */
public class Test {
    public static void main(String[] args) throws NacosException, InterruptedException {
        // 【模拟手动获取配置文件】
        String serverAddr = "localhost:8848";
        //String namespace = "013f54f2-582f-4921-a998-46fdabeb0542";
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        // 不设置命名空间时, 默认使用public命名空间
        //properties.put(PropertyKeyConst.NAMESPACE, namespace);
        // 创建ConfigService对象
        ConfigService configService = ConfigFactory.createConfigService(properties);

        String dataId = "nacos-config-study.properties";
        String group = "DEFAULT_GROUP";
        // 手动查询配置文件信息
        String config = configService.getConfig(dataId, group, 5000);
        System.out.println("原配置文件信息 -> \n" + config + "\n");



        // 【模拟配置文件变更及回调】
        // 添加监听器
        configService.addListener(dataId, group, new Listener(){

            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("receiveConfigInfo, configInfo=\n" + configInfo + "\n");
            }
        });

        // 手动修改配置文件. 查看监听事件回调
        String newConfigContent = "user.name=clj\n" +
                "user.age=188";
        configService.publishConfig(dataId, group, newConfigContent);

        // 手动查询配置文件信息
        config = configService.getConfig(dataId, group, 5000);
        System.out.println("修改后配置文件信息 -> \n" + config + "\n");

        // 阻塞, 以便看回调信息
        Thread.sleep(10000);

    }
}
