package org.example.study.baseSdk.database.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;

/** 在线测试记录 Mapper，SQL 保存在 XML。 */
public interface OnlineTestRecordMapper {
    int insert(OnlineTestRecordData record);

    List<OnlineTestRecordData> selectRecent(@Param("tenantId") long tenantId, @Param("limit") int limit);
}
