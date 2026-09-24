package org.example.study.baseSdk.database.mybatis.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleDraftCommand;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleSetCommand;
import org.example.study.baseSdk.database.api.model.SpelRuleDraftData;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.model.SpelRuleSetData;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleDraftEntity;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleReleaseEntity;
import org.example.study.baseSdk.database.mybatis.entity.SpelRuleSetEntity;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleDraftMapper;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleReleaseMapper;
import org.example.study.baseSdk.database.mybatis.mapper.SpelRuleSetMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 基于 MyBatis-Plus 的 SpEL 规则持久化实现。 */
@Service
public class SpelRuleDataServiceImpl implements SpelRuleDataService {

    private final SpelRuleSetMapper ruleSetMapper;
    private final SpelRuleDraftMapper draftMapper;
    private final SpelRuleReleaseMapper releaseMapper;

    public SpelRuleDataServiceImpl(SpelRuleSetMapper ruleSetMapper, SpelRuleDraftMapper draftMapper,
                                   SpelRuleReleaseMapper releaseMapper) {
        this.ruleSetMapper = ruleSetMapper;
        this.draftMapper = draftMapper;
        this.releaseMapper = releaseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpelRuleSetData> listRuleSets(long tenantId) {
        validateTenantId(tenantId);
        try {
            return ruleSetMapper.selectAllRuleSets(tenantId).stream().map(this::toRuleSetData).toList();
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule sets", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpelRuleSetData> findRuleSet(long tenantId, String ruleSetCode) {
        validateTenantId(tenantId);
        requireText(ruleSetCode, "SpEL rule set code must not be blank");
        try {
            return Optional.ofNullable(ruleSetMapper.selectByCode(tenantId, ruleSetCode)).map(this::toRuleSetData);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule set", exception);
        }
    }

    @Override
    @Transactional
    public SpelRuleSetData lockRuleSet(long tenantId, String ruleSetCode) {
        validateTenantId(tenantId);
        requireText(ruleSetCode, "SpEL rule set code must not be blank");
        try {
            SpelRuleSetEntity entity = ruleSetMapper.selectByCodeForUpdate(tenantId, ruleSetCode);
            if (entity == null) {
                throw new IllegalArgumentException("SpEL rule set does not exist: " + ruleSetCode);
            }
            return toRuleSetData(entity);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to lock SpEL rule set", exception);
        }
    }

    @Override
    @Transactional
    public SpelRuleSetData saveRuleSet(SaveSpelRuleSetCommand command, long expectedVersion) {
        validateRuleSetCommand(command, expectedVersion);
        try {
            SpelRuleSetEntity entity = ruleSetMapper.selectByCodeForUpdate(
                    command.tenantId(), command.ruleSetCode());
            if (entity == null) {
                if (expectedVersion != 0) {
                    throw new ConfigConflictException();
                }
                entity = new SpelRuleSetEntity();
                entity.setTenantId(command.tenantId());
                entity.setRuleSetCode(command.ruleSetCode());
                entity.setPublishedVersion(0L);
                entity.setVersion(1L);
                entity.setDeleted(0);
            } else if (!entity.getVersion().equals(expectedVersion)) {
                throw new ConfigConflictException();
            }
            entity.setRuleSetName(command.ruleSetName());
            entity.setScene(command.scene());
            entity.setEvaluationMode(command.evaluationMode());
            entity.setErrorPolicy(command.errorPolicy());
            entity.setEnabled(command.enabled());
            int affected = entity.getId() == null ? ruleSetMapper.insert(entity) : ruleSetMapper.updateById(entity);
            if (affected != 1) {
                throw new ConfigConflictException();
            }
            return toRuleSetData(entity);
        } catch (DatabaseSdkException exception) {
            throw exception;
        } catch (DuplicateKeyException exception) {
            throw new ConfigConflictException(exception);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to save SpEL rule set", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpelRuleDraftData> listDrafts(long tenantId, String ruleSetCode) {
        validateTenantId(tenantId);
        try {
            SpelRuleSetEntity ruleSet = requiredRuleSet(tenantId, ruleSetCode, false);
            return draftMapper.selectByRuleSetId(tenantId, ruleSet.getId()).stream()
                    .map(this::toDraftData).toList();
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule drafts", exception);
        }
    }

    @Override
    @Transactional
    public SpelRuleDraftData saveDraft(long tenantId, String ruleSetCode, SaveSpelRuleDraftCommand command,
                                       long expectedVersion) {
        validateTenantId(tenantId);
        validateDraftCommand(command, expectedVersion);
        try {
            SpelRuleSetEntity ruleSet = requiredRuleSet(tenantId, ruleSetCode, true);
            SpelRuleDraftEntity entity = draftMapper.selectByRuleId(tenantId, ruleSet.getId(), command.ruleId());
            if (entity == null) {
                if (expectedVersion != 0) {
                    throw new ConfigConflictException();
                }
                entity = new SpelRuleDraftEntity();
                entity.setTenantId(tenantId);
                entity.setRuleSetId(ruleSet.getId());
                entity.setRuleId(command.ruleId());
                entity.setVersion(1L);
                entity.setDeleted(0);
            } else if (!entity.getVersion().equals(expectedVersion)) {
                throw new ConfigConflictException();
            }
            entity.setRuleName(command.ruleName());
            entity.setExpressionText(command.expression());
            entity.setPriority(command.priority());
            entity.setAttributesJson(command.attributesJson());
            entity.setEnabled(command.enabled());
            int affected = entity.getId() == null ? draftMapper.insert(entity) : draftMapper.updateById(entity);
            if (affected != 1) {
                throw new ConfigConflictException();
            }
            return toDraftData(entity);
        } catch (DatabaseSdkException exception) {
            throw exception;
        } catch (DuplicateKeyException exception) {
            throw new ConfigConflictException(exception);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to save SpEL rule draft", exception);
        }
    }

    @Override
    @Transactional
    public SpelRuleReleaseData publish(long tenantId, String ruleSetCode, long expectedSetVersion, long releaseVersion,
                                       String snapshotJson, String checksum, boolean deleted, String publishedBy) {
        validateTenantId(tenantId);
        requireText(snapshotJson, "SpEL rule snapshot must not be blank");
        requireText(checksum, "SpEL rule snapshot checksum must not be blank");
        requireText(publishedBy, "SpEL rule publisher must not be blank");
        try {
            SpelRuleSetEntity ruleSet = requiredRuleSet(tenantId, ruleSetCode, true);
            if (!ruleSet.getVersion().equals(expectedSetVersion)
                    || releaseVersion != ruleSet.getPublishedVersion() + 1) {
                throw new ConfigConflictException();
            }
            SpelRuleReleaseEntity release = new SpelRuleReleaseEntity();
            release.setTenantId(tenantId);
            release.setRuleSetCode(ruleSetCode);
            release.setReleaseVersion(releaseVersion);
            release.setScene(ruleSet.getScene());
            release.setSnapshotJson(snapshotJson);
            release.setChecksum(checksum);
            release.setDeleted(deleted);
            release.setPublishedBy(publishedBy);
            release.setPublishedAt(LocalDateTime.now());
            if (releaseMapper.insert(release) != 1) {
                throw new DatabaseSdkException("Failed to insert SpEL rule release");
            }
            ruleSet.setPublishedVersion(releaseVersion);
            if (ruleSetMapper.updateById(ruleSet) != 1) {
                throw new ConfigConflictException();
            }
            return toReleaseData(release);
        } catch (DatabaseSdkException exception) {
            throw exception;
        } catch (DuplicateKeyException exception) {
            throw new ConfigConflictException(exception);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to publish SpEL rule set", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpelRuleReleaseData> listReleases(long tenantId, String ruleSetCode) {
        validateTenantId(tenantId);
        requireText(ruleSetCode, "SpEL rule set code must not be blank");
        try {
            return releaseMapper.selectByRuleSetCode(tenantId, ruleSetCode).stream()
                    .map(this::toReleaseData).toList();
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule releases", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpelRuleReleaseData> findRelease(long tenantId, String ruleSetCode, long releaseVersion) {
        validateTenantId(tenantId);
        requireText(ruleSetCode, "SpEL rule set code must not be blank");
        if (releaseVersion <= 0) {
            throw new IllegalArgumentException("SpEL release version must be greater than zero");
        }
        try {
            return Optional.ofNullable(releaseMapper.selectByVersion(tenantId, ruleSetCode, releaseVersion))
                    .map(this::toReleaseData);
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule release", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpelRuleReleaseData> listLatestReleases(long tenantId, String scene) {
        validateTenantId(tenantId);
        requireText(scene, "SpEL rule scene must not be blank");
        try {
            return releaseMapper.selectLatestByScene(tenantId, scene).stream().map(this::toReleaseData).toList();
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query latest SpEL rule releases", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpelRuleReleaseData> listReleaseChanges(long tenantId, String scene, long afterCursor, int limit) {
        validateTenantId(tenantId);
        requireText(scene, "SpEL rule scene must not be blank");
        if (afterCursor < 0 || limit <= 0 || limit > 500) {
            throw new IllegalArgumentException("Invalid SpEL release cursor query");
        }
        try {
            return releaseMapper.selectChanges(tenantId, scene, afterCursor, limit).stream()
                    .map(this::toReleaseData).toList();
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule release changes", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long maxReleaseCursor(long tenantId, String scene) {
        validateTenantId(tenantId);
        requireText(scene, "SpEL rule scene must not be blank");
        try {
            Long cursor = releaseMapper.selectMaxCursor(tenantId, scene);
            return cursor == null ? 0 : cursor;
        } catch (DataAccessException exception) {
            throw databaseFailure("Failed to query SpEL rule release cursor", exception);
        }
    }

    private SpelRuleSetEntity requiredRuleSet(long tenantId, String ruleSetCode, boolean lock) {
        requireText(ruleSetCode, "SpEL rule set code must not be blank");
        SpelRuleSetEntity entity = lock
                ? ruleSetMapper.selectByCodeForUpdate(tenantId, ruleSetCode)
                : ruleSetMapper.selectByCode(tenantId, ruleSetCode);
        if (entity == null) {
            throw new IllegalArgumentException("SpEL rule set does not exist: " + ruleSetCode);
        }
        return entity;
    }

    private void validateRuleSetCommand(SaveSpelRuleSetCommand command, long expectedVersion) {
        if (command == null || expectedVersion < 0) {
            throw new IllegalArgumentException("Invalid SpEL rule set save command");
        }
        validateTenantId(command.tenantId());
        requireText(command.ruleSetCode(), "SpEL rule set code must not be blank");
        requireText(command.ruleSetName(), "SpEL rule set name must not be blank");
        requireText(command.scene(), "SpEL rule scene must not be blank");
        requireText(command.evaluationMode(), "SpEL evaluation mode must not be blank");
        requireText(command.errorPolicy(), "SpEL error policy must not be blank");
    }

    private void validateDraftCommand(SaveSpelRuleDraftCommand command, long expectedVersion) {
        if (command == null || expectedVersion < 0) {
            throw new IllegalArgumentException("Invalid SpEL rule draft save command");
        }
        requireText(command.ruleId(), "SpEL rule ID must not be blank");
        requireText(command.ruleName(), "SpEL rule name must not be blank");
        requireText(command.expression(), "SpEL rule expression must not be blank");
        requireText(command.attributesJson(), "SpEL rule attributes must not be blank");
    }

    private SpelRuleSetData toRuleSetData(SpelRuleSetEntity entity) {
        return new SpelRuleSetData(entity.getId(), entity.getTenantId(), entity.getRuleSetCode(),
                entity.getRuleSetName(),
                entity.getScene(), entity.getEvaluationMode(), entity.getErrorPolicy(), entity.getPublishedVersion(),
                entity.getVersion(), Boolean.TRUE.equals(entity.getEnabled()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private SpelRuleDraftData toDraftData(SpelRuleDraftEntity entity) {
        return new SpelRuleDraftData(entity.getId(), entity.getTenantId(), entity.getRuleSetId(),
                entity.getRuleId(), entity.getRuleName(),
                entity.getExpressionText(), entity.getPriority(), entity.getAttributesJson(), entity.getVersion(),
                Boolean.TRUE.equals(entity.getEnabled()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private SpelRuleReleaseData toReleaseData(SpelRuleReleaseEntity entity) {
        return new SpelRuleReleaseData(entity.getReleaseId(), entity.getTenantId(), entity.getRuleSetCode(),
                entity.getReleaseVersion(),
                entity.getScene(), entity.getSnapshotJson(), entity.getChecksum(), Boolean.TRUE.equals(entity.getDeleted()),
                entity.getPublishedBy(), entity.getPublishedAt());
    }

    private DatabaseSdkException databaseFailure(String message, DataAccessException exception) {
        return new DatabaseSdkException(message, exception);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateTenantId(long tenantId) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }
    }
}
