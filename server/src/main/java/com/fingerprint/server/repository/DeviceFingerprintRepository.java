package com.fingerprint.server.repository;

import com.fingerprint.server.model.DeviceFingerprintDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 设备指纹索引仓储接口。
 */
@Repository
public interface DeviceFingerprintRepository extends ElasticsearchRepository<DeviceFingerprintDocument, String> {

    /**
     * 根据 FingerprintJS 访客标识查询文档。
     *
     * @param visitorId 访客标识
     * @return 匹配的文档
     */
    Optional<DeviceFingerprintDocument> findTopByVisitorIdOrderByUpdatedAtDesc(String visitorId);
}
