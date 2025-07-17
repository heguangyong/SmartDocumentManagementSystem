package com.github.sdms.util;

import com.github.sdms.model.IdSequenceEntity;
import com.github.sdms.repository.IdSequenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class CachedIdGenerator {

    private final IdSequenceRepository idSequenceRepository;

    // 每个类型一组缓存
    private final ConcurrentHashMap<String, AtomicLong> cacheMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> maxMap = new ConcurrentHashMap<>();

    // 单次预分配多少个
    private static final int BATCH_SIZE = 100;

    @Transactional
    public synchronized long nextId(String type) {
        AtomicLong counter = cacheMap.computeIfAbsent(type, k -> new AtomicLong(0));
        Long max = maxMap.getOrDefault(type, 0L);

        // 缓存已用完，尝试从数据库预取
        if (counter.get() >= max) {
            // 从数据库读取并尝试更新
            IdSequenceEntity entity = idSequenceRepository.findByType(type)
                    .orElseGet(() -> {
                        IdSequenceEntity newEntity = new IdSequenceEntity();
                        newEntity.setType(type);
                        newEntity.setCurrentValue(0L);
                        return idSequenceRepository.save(newEntity);
                    });

            long oldVal = entity.getCurrentValue();
            long newVal = oldVal + BATCH_SIZE;

            // 乐观锁方式更新
            int updated = idSequenceRepository.updateCurrentValue(type, oldVal, newVal);
            if (updated == 0) {
                throw new RuntimeException("获取ID失败：并发冲突，请重试");
            }

            cacheMap.put(type, new AtomicLong(oldVal + 1));
            maxMap.put(type, newVal);
        }

        return cacheMap.get(type).getAndIncrement();
    }
}
