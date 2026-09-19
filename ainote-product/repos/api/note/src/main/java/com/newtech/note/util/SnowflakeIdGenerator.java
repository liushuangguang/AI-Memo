package com.newtech.note.util;

import lombok.Getter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SnowflakeIdGenerator {
    // 单例模式，返回类实例
    // 使用 ConcurrentHashMap 代替 HashMap，保证线程安全
    @Getter
    private static final SnowflakeIdGenerator instance = new SnowflakeIdGenerator();

    // 机器ID部分的位数
    private static final int MACHINE_ID_BITS = 10;
    // 序列号部分的位数
    private static final int SEQUENCE_BITS = 12;
    // 序列号最大值
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    // 时间戳左移位数
    private static final int TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    // 机器ID左移位数
    private static final int MACHINE_ID_LEFT_SHIFT = SEQUENCE_BITS;

    private final long machineId; // 机器ID
    private long sequence = 0; // 序列号
    private long lastTimestamp = -1L; // 最后生成ID的时间戳
    private static final String FULL_ID_FORMATTER = "urn:%s:%s";

    private static final Map<String, Long> businessMachineIdCache = new ConcurrentHashMap<>();

    private SnowflakeIdGenerator() {
        this.machineId = getMachineId();
    }

    // 泛型方法，生成完整的ID
    public <T> String nextFullId(Class<T> clazz) {
        return FULL_ID_FORMATTER.formatted(clazz.getSimpleName().toLowerCase(), nextId(clazz));
    }

    // 泛型方法，生成唯一ID
    public synchronized <T> long nextId(Class<T> clazz) {
        // Validate the supported business type, but keep the 10-bit machine field machine-only.
        // The sequence is shared across all business types, so adding the business id here is
        // unnecessary and can overlap the timestamp bits, producing duplicate or decreasing ids.
        getBusinessMachineId(clazz.getName());
        long combinedMachineId = machineId;
        long timestamp = System.currentTimeMillis();
        // 检查系统时钟是否倒退
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("系统时钟倒退，无法生成ID");
        }
        // 同一毫秒内生成序列号
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0; // 不同时间戳，序列号重置
        }
        lastTimestamp = timestamp;
        // 组合ID
        return ((timestamp << TIMESTAMP_LEFT_SHIFT) | (combinedMachineId << MACHINE_ID_LEFT_SHIFT) | sequence);
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private static long getMachineId() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            String[] ipParts = ipAddress.split("\\.");

            int part1 = Integer.parseInt(ipParts[0]);
            int part2 = Integer.parseInt(ipParts[1]);
            int part3 = Integer.parseInt(ipParts[2]);
            int part4 = Integer.parseInt(ipParts[3]);

            // 对每个部分分别应用哈希算法
            long hash1 = hash(part1);
            long hash2 = hash(part2);
            long hash3 = hash(part3);
            long hash4 = hash(part4);

            // 合并哈希值，并限制结果范围在100到999之间
            return (hash1 + hash2 + hash3 + hash4) % 900 + 100;
        } catch (UnknownHostException e) {
            throw new RuntimeException("无法获取机器ID", e);
        }
    }

    private static long hash(int input) {
        return (input * 31L + 17L) % 1000; // 一个简单的哈希函数
    }

    // 优化业务类型查找性能，使用缓存
    private long getBusinessMachineId(String className) {
        return businessMachineIdCache.computeIfAbsent(className, BusinessMachineId::getBusinessMachineId);
    }

    @Getter
    public enum BusinessMachineId {
        NOTE_ANALYSIS("com.newtech.note.entity.dto.NoteAnalysis", 1),
        NOTE_ANALYSIS_HISTORY("com.newtech.note.entity.dto.NoteAnalysisHistory", 2),
        NOTE("com.newtech.note.entity.dto.Note", 3),
        NOTE_MODULE("com.newtech.note.entity.dto.noteModules.NoteModule", 4),
        BACKLOG("com.newtech.note.entity.dto.Backlog", 5),
        NOTE_MODULE_ITEM("com.newtech.note.entity.dto.noteModules.items.NoteModuleItem", 6),
        NOTE_ANALYSIS_RECORD("com.newtech.note.entity.dto.NoteAnalysisRecord", 7),
        NOTE_ASSIST_RECORD("com.newtech.note.entity.dto.NoteAssistRecord", 8),
        NOTE_THEME("com.newtech.note.entity.dto.NoteTheme", 9),
        USER_INFO("com.newtech.note.entity.dto.UserInfo", 10);

        private final String businessType;
        private final int businessMachineId;

        private static final Map<String, BusinessMachineId> BUSINESS_TYPE_MAP = new ConcurrentHashMap<>();

        static {
            for (BusinessMachineId businessMachineId : BusinessMachineId.values()) {
                BUSINESS_TYPE_MAP.put(businessMachineId.getBusinessType(), businessMachineId);
            }
        }

        BusinessMachineId(String businessType, int businessMachineId) {
            this.businessType = businessType;
            this.businessMachineId = businessMachineId;
        }

        public static long getBusinessMachineId(String className) {
            BusinessMachineId businessMachineId = BUSINESS_TYPE_MAP.get(className);
            if (businessMachineId == null) {
                throw new IllegalArgumentException("无效的业务类型: " + className);
            }
            return businessMachineId.getBusinessMachineId();
        }
    }
}
