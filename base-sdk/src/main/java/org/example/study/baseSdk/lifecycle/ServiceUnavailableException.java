package org.example.study.baseSdk.lifecycle;

/** 服务未就绪或正在下线时，拒绝新的业务任务。 */
public class ServiceUnavailableException extends IllegalStateException {
    public ServiceUnavailableException(ServiceState state) {
        super("Service is not accepting new work: " + state);
    }
}
