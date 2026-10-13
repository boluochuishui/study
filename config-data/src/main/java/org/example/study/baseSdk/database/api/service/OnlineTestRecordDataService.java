package org.example.study.baseSdk.database.api.service;

import java.util.List;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;

/** 管理台在线测试记录的数据边界。 */
public interface OnlineTestRecordDataService {
    void save(OnlineTestRecordData record);

    List<OnlineTestRecordData> list(long tenantId, int limit);
}
