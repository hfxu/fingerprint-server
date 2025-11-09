package com.fingerprint.server.service;

import com.fingerprint.server.config.SimilarityProperties;
import com.fingerprint.server.dto.FingerprintRequest;
import com.fingerprint.server.dto.FingerprintResponse;
import com.fingerprint.server.mapper.DeviceFingerprintMapper;
import com.fingerprint.server.model.DeviceFingerprintDocument;
import com.fingerprint.server.repository.DeviceFingerprintRepository;
import com.fingerprint.server.service.support.SimilarityScorer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 设备指纹业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintService.class);

    private final DeviceFingerprintRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DeviceFingerprintMapper mapper;
    private final SimilarityScorer similarityScorer;
    private final SimilarityProperties similarityProperties;

    /**
     * 处理新上报的设备指纹。
     *
     * @param request 指纹上报请求
     * @return 匹配结果
     */
    @Transactional
    public FingerprintResponse handleFingerprint(FingerprintRequest request) {
        DeviceFingerprintDocument incoming = mapper.toDocument(request);
        CandidateMatch candidateMatch = findBestMatch(incoming).orElse(null);
        Instant now = Instant.now();

        if (candidateMatch != null && candidateMatch.score() >= similarityProperties.getThreshold()) {
            DeviceFingerprintDocument matched = candidateMatch.document();
            mergeFingerprint(matched, incoming, now, candidateMatch.score());
            repository.save(matched);
            log.info("Fingerprint matched with existing device: id={} score={}", matched.getId(), candidateMatch.score());
            return new FingerprintResponse(
                    matched.getId(),
                    true,
                    candidateMatch.score(),
                    matched.getId(),
                    now,
                    Map.of(
                            "observationCount", Optional.ofNullable(matched.getObservationCount()).orElse(0),
                            "ipHistorySize", matched.getIpHistory() == null ? 0 : matched.getIpHistory().size())
            );
        }

        String newId = UUID.randomUUID().toString();
        incoming.setId(newId);
        incoming.setCreatedAt(now);
        incoming.setUpdatedAt(now);
        incoming.setSimilarityScore(1d);
        repository.save(incoming);
        log.info("Fingerprint stored as new device: id={}", newId);
        return new FingerprintResponse(
                newId,
                false,
                0d,
                null,
                now,
                Map.of("observationCount", incoming.getObservationCount())
        );
    }

    private Optional<CandidateMatch> findBestMatch(DeviceFingerprintDocument incoming) {
        Set<String> visitedIds = new HashSet<>();
        List<DeviceFingerprintDocument> candidates = new ArrayList<>();

        repository.findTopByVisitorIdOrderByUpdatedAtDesc(incoming.getVisitorId())
                .ifPresent(doc -> {
                    candidates.add(doc);
                    visitedIds.add(doc.getId());
                });

        for (DeviceFingerprintDocument doc : searchCandidates(incoming)) {
            if (doc.getId() != null && !visitedIds.contains(doc.getId())) {
                candidates.add(doc);
                visitedIds.add(doc.getId());
            }
        }

        CandidateMatch best = null;
        for (DeviceFingerprintDocument candidate : candidates) {
            double score = similarityScorer.calculate(incoming, candidate);
            if (best == null || score > best.score()) {
                best = new CandidateMatch(candidate, score);
            }
        }
        return Optional.ofNullable(best);
    }

    private List<DeviceFingerprintDocument> searchCandidates(DeviceFingerprintDocument incoming) {
        List<Criteria> criteriaList = new ArrayList<>();
        if (StringUtils.isNotBlank(incoming.getVisitorId())) {
            criteriaList.add(Criteria.where("visitorId").is(incoming.getVisitorId()));
        }
        if (incoming.getNetwork() != null && StringUtils.isNotBlank(incoming.getNetwork().getIpAddress())) {
            criteriaList.add(Criteria.where("network.ipAddress").is(incoming.getNetwork().getIpAddress()));
        }
        if (incoming.getCertificate() != null && incoming.getCertificate().getFingerprints() != null) {
            incoming.getCertificate().getFingerprints().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(fp -> Criteria.where("certificate.fingerprints").is(fp))
                    .forEach(criteriaList::add);
        }
        if (incoming.getBrowser() != null && StringUtils.isNotBlank(incoming.getBrowser().getCanvasFingerprint())) {
            criteriaList.add(Criteria.where("browser.canvasFingerprint").is(incoming.getBrowser().getCanvasFingerprint()));
        }

        if (criteriaList.isEmpty()) {
            return List.of();
        }

        Criteria combined = criteriaList.get(0);
        for (int i = 1; i < criteriaList.size(); i++) {
            combined = combined.or(criteriaList.get(i));
        }

        CriteriaQuery query = new CriteriaQuery(combined);
        query.setMaxResults(20);
        SearchHits<DeviceFingerprintDocument> searchHits = elasticsearchOperations.search(query, DeviceFingerprintDocument.class);
        List<DeviceFingerprintDocument> results = new ArrayList<>();
        for (SearchHit<DeviceFingerprintDocument> hit : searchHits) {
            results.add(hit.getContent());
        }
        return results;
    }

    private void mergeFingerprint(DeviceFingerprintDocument target,
                                  DeviceFingerprintDocument incoming,
                                  Instant now,
                                  double similarityScore) {
        target.setSimilarityScore(similarityScore);
        target.setUpdatedAt(now);
        target.setObservationCount(Optional.ofNullable(target.getObservationCount()).orElse(0) + 1);
        mergeIpHistory(target, incoming);
        if (incoming.getBrowser() != null) {
            target.setBrowser(incoming.getBrowser());
        }
        if (incoming.getDevice() != null) {
            target.setDevice(incoming.getDevice());
        }
        if (incoming.getNetwork() != null) {
            target.setNetwork(incoming.getNetwork());
        }
        if (incoming.getGeoLocation() != null) {
            target.setGeoLocation(incoming.getGeoLocation());
        }
        if (incoming.getCertificate() != null) {
            target.setCertificate(incoming.getCertificate());
        }
        if (incoming.getMetadata() != null && !incoming.getMetadata().isEmpty()) {
            if (target.getMetadata() == null) {
                target.setMetadata(incoming.getMetadata());
            } else {
                target.getMetadata().putAll(incoming.getMetadata());
            }
        }
    }

    private void mergeIpHistory(DeviceFingerprintDocument target, DeviceFingerprintDocument incoming) {
        if (incoming.getNetwork() == null || StringUtils.isBlank(incoming.getNetwork().getIpAddress())) {
            return;
        }
        List<String> ipHistory = target.getIpHistory();
        if (ipHistory == null) {
            ipHistory = new ArrayList<>();
            target.setIpHistory(ipHistory);
        }
        String ip = incoming.getNetwork().getIpAddress();
        if (!ipHistory.contains(ip)) {
            ipHistory.add(0, ip);
            if (ipHistory.size() > 20) {
                ipHistory.remove(ipHistory.size() - 1);
            }
        }
    }

    private record CandidateMatch(DeviceFingerprintDocument document, double score) {
    }
}
