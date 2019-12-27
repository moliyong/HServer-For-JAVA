package top.hserver.cloud;


import lombok.extern.slf4j.Slf4j;
import top.hserver.cloud.server.handler.RegServer;
import top.hserver.cloud.task.BroadcastTask;
import top.hserver.cloud.util.NetUtil;
import top.hserver.core.task.TaskManager;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class CloudManager {

    public static void run() {
        //1.读取自己是不是开启了云
        try {
            Properties pps = new Properties();
            InputStream resourceAsStream = CloudManager.class.getResourceAsStream("/application.properties");
            pps.load(resourceAsStream);
            Object open = pps.get("app.cloud.open");
            if (open!=null&&Boolean.valueOf(open.toString())) {
                //2.自己是不是主机
                Object master_open = pps.get("app.cloud.master.open");
                if (master_open!=null&&Boolean.valueOf(master_open.toString())){
                    //开启监听从机动态
                    new RegServer().start();
                }

                //自己是不是从机
                Object slave_open = pps.get("app.cloud.slave.open");
                if (slave_open!=null&&Boolean.valueOf(slave_open.toString())){
                    //上报给主机自己的状态
                    Object cloud_name = pps.get("app.cloud.name");
                    if (cloud_name==null){
                        //获取内网IP
                        cloud_name=NetUtil.getIpAddress();
                    }else {
                        cloud_name=cloud_name+"-->"+NetUtil.getIpAddress();
                    }
                    TaskManager.addTask(cloud_name.toString(),5000,BroadcastTask.class,cloud_name.toString());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}