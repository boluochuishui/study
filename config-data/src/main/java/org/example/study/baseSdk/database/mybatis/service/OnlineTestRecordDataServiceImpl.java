package org.example.study.baseSdk.database.mybatis.service;

import java.util.List;
import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;
import org.example.study.baseSdk.database.api.service.OnlineTestRecordDataService;
import org.example.study.baseSdk.database.mybatis.mapper.OnlineTestRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

/** 管理台记录持久化实现。 */
@Service
public class OnlineTestRecordDataServiceImpl implements OnlineTestRecordDataService {
    private final OnlineTestRecordMapper mapper;

    public OnlineTestRecordDataServiceImpl(OnlineTestRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(OnlineTestRecordData record) {
        try {
            mapper.insert(record);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to save online test record", exception);
        }
    }

    @Override
    public List<OnlineTestRecordData> list(long tenantId, int limit) {
        if (tenantId <= 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid online test record query");
        }
        try {
            return mapper.selectRecent(tenantId, limit);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to query online test records", exception);
        }
    }
}
