package org.example.study.worker.lifecycle;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.command.impl.Offline;
import org.apache.dubbo.qos.command.impl.OfflineApp;
import org.example.study.baseSdk.lifecycle.LifecycleParticipant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 检查 RPC 导出状态，摘流时同时注销接口级和应用级注册，保持在途连接可用。 */
@Component
@Order(10)
public class DubboLifecycleParticipant implements LifecycleParticipant {
    private final ObjectProvider<ServiceConfig<?>> services;

    public DubboLifecycleParticipant(ObjectProvider<ServiceConfig<?>> services) {
        this.services = services;
    }

    @Override
    public void prepare() {
        services.stream().forEach(service -> {
            if (!service.isExported()) {
                throw new IllegalStateException("Detection RPC service is not exported: " + service.getInterface());
            }
        });
    }

    @Override
    public void onDrain() {
        services.stream().map(service -> service.getScopeModel().getApplicationModel().getFrameworkModel()).distinct().forEach(framework -> {
            new Offline(framework).execute(new CommandContext("offline"), new String[]{".*"});
            new OfflineApp(framework).execute(new CommandContext("offlineApp"), new String[]{".*"});
        });
    }
}
