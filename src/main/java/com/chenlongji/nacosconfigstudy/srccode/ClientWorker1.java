/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *//*


package com.chenlongji.nacosconfigstudy.srccode;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.common.GroupKey;
import com.alibaba.nacos.client.config.filter.impl.ConfigFilterChainManager;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.client.config.impl.CacheData;
import com.alibaba.nacos.client.config.impl.LocalConfigInfoProcessor;
import com.alibaba.nacos.client.config.utils.ContentUtils;
import com.alibaba.nacos.client.monitor.MetricsMonitor;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.client.utils.LogUtils;
import com.alibaba.nacos.client.utils.ParamUtil;
import com.alibaba.nacos.client.utils.TenantUtil;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.lifecycle.Closeable;
import com.alibaba.nacos.common.utils.ConvertUtils;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.common.utils.ThreadUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;

import static com.alibaba.nacos.api.common.Constants.*;

*/
/**
 * Long polling.
 *
 * @author Nacos
 *//*

public class ClientWorker1 implements Closeable {

    private static final Logger LOGGER = LogUtils.logger(ClientWorker1.class);

    */
/**
     * Add listeners for data.
     *
     * @param dataId    dataId of data
     * @param group     group of data
     * @param listeners listeners
     *//*

    public void addListeners(String dataId, String group, List<? extends Listener> listeners) {
        group = null2defaultGroup(group);
        CacheData cache = addCacheDataIfAbsent(dataId, group);
        for (Listener listener : listeners) {
            cache.addListener(listener);
        }
    }

    */
/**
     * Remove listener.
     *
     * @param dataId   dataId of data
     * @param group    group of data
     * @param listener listener
     *//*

    public void removeListener(String dataId, String group, Listener listener) {
        group = null2defaultGroup(group);
        CacheData cache = getCache(dataId, group);
        if (null != cache) {
            cache.removeListener(listener);
            if (cache.getListeners().isEmpty()) {
                removeCache(dataId, group);
            }
        }
    }

    */
/**
     * Add listeners for tenant.
     *
     * @param dataId    dataId of data
     * @param group     group of data
     * @param listeners listeners
     * @throws NacosException nacos exception
     *//*

    public void addTenantListeners(String dataId, String group, List<? extends Listener> listeners)
            throws NacosException {
        group = null2defaultGroup(group);
        String tenant = agent.getTenant();
        CacheData cache = addCacheDataIfAbsent(dataId, group, tenant);
        for (Listener listener : listeners) {
            cache.addListener(listener);
        }
    }

    */
/**
     * Add listeners for tenant with content.
     *
     * @param dataId    dataId of data
     * @param group     group of data
     * @param content   content
     * @param listeners listeners
     * @throws NacosException nacos exception
     *//*

    public void addTenantListenersWithContent(String dataId, String group, String content,
            List<? extends Listener> listeners) throws NacosException {
        group = null2defaultGroup(group);
        String tenant = agent.getTenant();
        CacheData cache = addCacheDataIfAbsent(dataId, group, tenant);
        cache.setContent(content);
        for (Listener listener : listeners) {
            cache.addListener(listener);
        }
    }

    */
/**
     * Remove listeners for tenant.
     *
     * @param dataId   dataId of data
     * @param group    group of data
     * @param listener listener
     *//*

    public void removeTenantListener(String dataId, String group, Listener listener) {
        group = null2defaultGroup(group);
        String tenant = agent.getTenant();
        CacheData cache = getCache(dataId, group, tenant);
        if (null != cache) {
            cache.removeListener(listener);
            if (cache.getListeners().isEmpty()) {
                removeCache(dataId, group, tenant);
            }
        }
    }

    private void removeCache(String dataId, String group) {
        String groupKey = GroupKey.getKey(dataId, group);
        cacheMap.remove(groupKey);
        LOGGER.info("[{}] [unsubscribe] {}", this.agent.getName(), groupKey);
        MetricsMonitor.getListenConfigCountMonitor().set(cacheMap.size());
    }

    void removeCache(String dataId, String group, String tenant) {
        String groupKey = GroupKey.getKeyTenant(dataId, group, tenant);
        cacheMap.remove(groupKey);
        LOGGER.info("[{}] [unsubscribe] {}", agent.getName(), groupKey);

        MetricsMonitor.getListenConfigCountMonitor().set(cacheMap.size());
    }

    */
/**
     * Add cache data if absent.
     *
     * @param dataId data id if data
     * @param group  group of data
     * @return cache data
     *//*

    public CacheData addCacheDataIfAbsent(String dataId, String group) {
        String key = GroupKey.getKey(dataId, group);
        CacheData cacheData = cacheMap.get(key);
        if (cacheData != null) {
            return cacheData;
        }

        cacheData = new CacheData(configFilterChainManager, agent.getName(), dataId, group);
        // multiple listeners on the same dataid+group and race condition
        CacheData lastCacheData = cacheMap.putIfAbsent(key, cacheData);
        if (lastCacheData == null) {
            int taskId = cacheMap.size() / (int) ParamUtil.getPerTaskConfigSize();
            lastCacheData = cacheData;
            lastCacheData.setTaskId(taskId);
        }
        // reset so that server not hang this check
        lastCacheData.setInitializing(true);

        LOGGER.info("[{}] [subscribe] {}", this.agent.getName(), key);

        MetricsMonitor.getListenConfigCountMonitor().set(cacheMap.size());
        return lastCacheData;
    }

    */
/**
     * Add cache data if absent.
     *
     * @param dataId data id if data
     * @param group  group of data
     * @param tenant tenant of data
     * @return cache data
     *//*

    public CacheData addCacheDataIfAbsent(String dataId, String group, String tenant) throws NacosException {
        String key = GroupKey.getKeyTenant(dataId, group, tenant);
        CacheData cacheData = cacheMap.get(key);
        if (cacheData != null) {
            return cacheData;
        }

        cacheData = new CacheData(configFilterChainManager, agent.getName(), dataId, group, tenant);
        // multiple listeners on the same dataid+group and race condition
        CacheData lastCacheData = cacheMap.putIfAbsent(key, cacheData);
        if (lastCacheData == null) {
            //fix issue # 1317
            if (enableRemoteSyncConfig) {
                String[] ct = getServerConfig(dataId, group, tenant, 3000L);
                cacheData.setContent(ct[0]);
            }
            int taskId = cacheMap.size() / (int) ParamUtil.getPerTaskConfigSize();
            cacheData.setTaskId(taskId);
            lastCacheData = cacheData;
        }

        // reset so that server not hang this check
        lastCacheData.setInitializing(true);

        LOGGER.info("[{}] [subscribe] {}", agent.getName(), key);
        MetricsMonitor.getListenConfigCountMonitor().set(cacheMap.size());

        return lastCacheData;
    }

    public CacheData getCache(String dataId, String group) {
        return getCache(dataId, group, TenantUtil.getUserTenantForAcm());
    }

    public CacheData getCache(String dataId, String group, String tenant) {
        if (null == dataId || null == group) {
            throw new IllegalArgumentException();
        }
        return cacheMap.get(GroupKey.getKeyTenant(dataId, group, tenant));
    }

    */
/**
     * 核心代码
     *//*

    public String[] getServerConfig(String dataId, String group, String tenant, long readTimeout)
            throws NacosException {
        String[] ct = new String[2];
        if (StringUtils.isBlank(group)) {
            group = Constants.DEFAULT_GROUP;
        }

        HttpRestResult<String> result = null;
        try {
            // 构建请求参数
            Map<String, String> params = new HashMap<String, String>(3);
            if (StringUtils.isBlank(tenant)) {
                params.put("dataId", dataId);
                params.put("group", group);
            } else {
                params.put("dataId", dataId);
                params.put("group", group);
                params.put("tenant", tenant);
            }
            // 请求服务端接口 /v1/cs/configs
            result = agent.httpGet(Constants.CONFIG_CONTROLLER_PATH, null, params, agent.getEncode(), readTimeout);
        } catch (Exception ex) {
            String message = String
                    .format("[%s] [sub-server] get server config exception, dataId=%s, group=%s, tenant=%s",
                            agent.getName(), dataId, group, tenant);
            LOGGER.error(message, ex);
            throw new NacosException(NacosException.SERVER_ERROR, ex);
        }

        switch (result.getCode()) {
            case HttpURLConnection.HTTP_OK:
                // 请求成功, 将配置文件写入本地快照
                LocalConfigInfoProcessor.saveSnapshot(agent.getName(), dataId, group, tenant, result.getData());
                ct[0] = result.getData();
                if (result.getHeader().getValue(CONFIG_TYPE) != null) {
                    ct[1] = result.getHeader().getValue(CONFIG_TYPE);
                } else {
                    ct[1] = ConfigType.TEXT.getType();
                }
                return ct;
            case HttpURLConnection.HTTP_NOT_FOUND:
                // 服务端找不到配置文件, 则删除本地快照
                LocalConfigInfoProcessor.saveSnapshot(agent.getName(), dataId, group, tenant, null);
                return ct;
            case HttpURLConnection.HTTP_CONFLICT: {
                LOGGER.error(
                        "[{}] [sub-server-error] get server config being modified concurrently, dataId={}, group={}, "
                                + "tenant={}", agent.getName(), dataId, group, tenant);
                throw new NacosException(NacosException.CONFLICT,
                        "data being modified, dataId=" + dataId + ",group=" + group + ",tenant=" + tenant);
            }
            case HttpURLConnection.HTTP_FORBIDDEN: {
                LOGGER.error("[{}] [sub-server-error] no right, dataId={}, group={}, tenant={}", agent.getName(),
                        dataId, group, tenant);
                throw new NacosException(result.getCode(), result.getMessage());
            }
            default: {
                LOGGER.error("[{}] [sub-server-error]  dataId={}, group={}, tenant={}, code={}", agent.getName(),
                        dataId, group, tenant, result.getCode());
                throw new NacosException(result.getCode(),
                        "http error, code=" + result.getCode() + ",dataId=" + dataId + ",group=" + group + ",tenant="
                                + tenant);
            }
        }
    }

    private void checkLocalConfig(CacheData cacheData) {
        final String dataId = cacheData.dataId;
        final String group = cacheData.group;
        final String tenant = cacheData.tenant;
        // 获取本地配置
        File path = LocalConfigInfoProcessor.getFailoverFile(agent.getName(), dataId, group, tenant);

        // 之前没有使用本地配置 && 本地配置存在时, 更新缓存为本地配置的数据
        if (!cacheData.isUseLocalConfigInfo() && path.exists()) {
            String content = LocalConfigInfoProcessor.getFailover(agent.getName(), dataId, group, tenant);
            final String md5 = MD5Utils.md5Hex(content, Constants.ENCODE);
            // 将缓存中的 版本号更新为文件最后更新时间, 更新配置内容、及设置使用本地配置标识为true
            cacheData.setUseLocalConfigInfo(true);
            cacheData.setLocalConfigInfoVersion(path.lastModified());
            // 设置内容是会重置md5的值
            cacheData.setContent(content);

            LOGGER.warn(
                    "[{}] [failover-change] failover file created. dataId={}, group={}, tenant={}, md5={}, content={}",
                    agent.getName(), dataId, group, tenant, md5, ContentUtils.truncateContent(content));
            return;
        }

        // 之前使用本地配置 && 本地配置已不存在, 则更新使用本地配置标识为false
        if (cacheData.isUseLocalConfigInfo() && !path.exists()) {
            cacheData.setUseLocalConfigInfo(false);
            LOGGER.warn("[{}] [failover-change] failover file deleted. dataId={}, group={}, tenant={}", agent.getName(),
                    dataId, group, tenant);
            return;
        }

        // 本地内容发生改变 (之前使用本地配置 && 本地配置存在 && 缓存的版本不等于本地配置最近更新时间)
        if (cacheData.isUseLocalConfigInfo() && path.exists() && cacheData.getLocalConfigInfoVersion() != path
                .lastModified()) {
            // 获取本地配置数据, 刷新缓存版本号、配置内容
            String content = LocalConfigInfoProcessor.getFailover(agent.getName(), dataId, group, tenant);
            final String md5 = MD5Utils.md5Hex(content, Constants.ENCODE);
            cacheData.setUseLocalConfigInfo(true);
            cacheData.setLocalConfigInfoVersion(path.lastModified());
            cacheData.setContent(content);
            LOGGER.warn(
                    "[{}] [failover-change] failover file changed. dataId={}, group={}, tenant={}, md5={}, content={}",
                    agent.getName(), dataId, group, tenant, md5, ContentUtils.truncateContent(content));
        }
    }

    private String null2defaultGroup(String group) {
        return (null == group) ? Constants.DEFAULT_GROUP : group.trim();
    }

    */
/**
     * Check config info.
     *//*

    public void checkConfigInfo() {
        // 将任务数按3000一组来分片
        int listenerSize = cacheMap.size();
        int longingTaskCount = (int) Math.ceil(listenerSize / ParamUtil.getPerTaskConfigSize());
        // 当前分片任务数 大于 执行中的分片任务数时, 为新的分片 启动执行任务LongPollingRunnable
        if (longingTaskCount > currentLongingTaskCount) {
            for (int i = (int) currentLongingTaskCount; i < longingTaskCount; i++) {
                // 启动一个长轮询的任务. taskId = i, 会检测cacheMap中taskId为i的所有配置文件
                executorService.execute(new LongPollingRunnable(i));
            }
            // 刷新当前执行中的分片任务数
            currentLongingTaskCount = longingTaskCount;
        }
    }

    */
/**
     * Fetch the dataId list from server.
     *
     * @param cacheDatas              CacheDatas for config infomations.
     * @param inInitializingCacheList initial cache lists.
     * @return String include dataId and group (ps: it maybe null).
     * @throws Exception Exception.
     *//*

    List<String> checkUpdateDataIds(List<CacheData> cacheDatas, List<String> inInitializingCacheList) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (CacheData cacheData : cacheDatas) {
            // 没有使用本地配置才处理.
            if (!cacheData.isUseLocalConfigInfo()) {
                // sb记录所有 没有使用本地配置的cacheData信息
                sb.append(cacheData.dataId).append(WORD_SEPARATOR);
                sb.append(cacheData.group).append(WORD_SEPARATOR);
                if (StringUtils.isBlank(cacheData.tenant)) {
                    sb.append(cacheData.getMd5()).append(LINE_SEPARATOR);
                } else {
                    sb.append(cacheData.getMd5()).append(WORD_SEPARATOR);
                    sb.append(cacheData.getTenant()).append(LINE_SEPARATOR);
                }
                // 收录cacheDatas中初始化中的cacheData
                if (cacheData.isInitializing()) {
                    // It updates when cacheData occours in cacheMap by first time.
                    inInitializingCacheList
                            .add(GroupKey.getKeyTenant(cacheData.dataId, cacheData.group, cacheData.tenant));
                }
            }
        }
        // 是否存在初始化中的cacheData
        boolean isInitializingCacheList = !inInitializingCacheList.isEmpty();
        // 调用服务端接口, 查询sb内的配置文件信息
        return checkUpdateConfigStr(sb.toString(), isInitializingCacheList);
    }

    */
/**
     * Fetch the updated dataId list from server.
     *
     * @param probeUpdateString       updated attribute string value.
     * @param isInitializingCacheList initial cache lists.
     * @return The updated dataId list(ps: it maybe null).
     * @throws IOException Exception.
     *//*

    List<String> checkUpdateConfigStr(String probeUpdateString, boolean isInitializingCacheList) throws Exception {

        Map<String, String> params = new HashMap<String, String>(2);
        // 添加所有关注的配置文件到Listening-Configs参数上
        params.put(Constants.PROBE_MODIFY_REQUEST, probeUpdateString);
        Map<String, String> headers = new HashMap<String, String>(2);
        // 设置长轮询超时时间
        headers.put("Long-Pulling-Timeout", "" + timeout);

        // 如果存在初始化中的cacheData, 告诉服务器本次请求不需要延迟(不需要长轮询)
        if (isInitializingCacheList) {
            headers.put("Long-Pulling-Timeout-No-Hangup", "true");
        }

        if (StringUtils.isBlank(probeUpdateString)) {
            return Collections.emptyList();
        }

        try {
            long readTimeoutMs = timeout + (long) Math.round(timeout >> 1);
            // 请求服务端接口 /v1/cs/configs/listener
            HttpRestResult<String> result = agent.httpPost(Constants.CONFIG_CONTROLLER_PATH + "/listener", headers, params, agent.getEncode(), readTimeoutMs);

            if (result.ok()) {
                // 更新服务端健康状态
                setHealthServer(true);
                // 解析已更新的配置文件基础信息列表
                return parseUpdateDataIdResponse(result.getData());
            } else {
                setHealthServer(false);
                LOGGER.error("[{}] [check-update] get changed dataId error, code: {}", agent.getName(), result.getCode());
            }
        } catch (Exception e) {
            setHealthServer(false);
            LOGGER.error("[" + agent.getName() + "] [check-update] get changed dataId exception", e);
            throw e;
        }
        return Collections.emptyList();
    }

    */
/**
     * Get the groupKey list from the http response.
     *
     * @param response Http response.
     * @return GroupKey List, (ps: it maybe null).
     *//*

    private List<String> parseUpdateDataIdResponse(String response) {
        if (StringUtils.isBlank(response)) {
            return Collections.emptyList();
        }

        try {
            response = URLDecoder.decode(response, "UTF-8");
        } catch (Exception e) {
            LOGGER.error("[" + agent.getName() + "] [polling-resp] decode modifiedDataIdsString error", e);
        }

        List<String> updateList = new LinkedList<String>();
        // 遍历返回的已变化的配置文件基础信息, 构建成key添加到updateList返回, 以便后续发起主动查询
        for (String dataIdAndGroup : response.split(LINE_SEPARATOR)) {
            if (!StringUtils.isBlank(dataIdAndGroup)) {
                String[] keyArr = dataIdAndGroup.split(WORD_SEPARATOR);
                String dataId = keyArr[0];
                String group = keyArr[1];
                if (keyArr.length == 2) {
                    updateList.add(GroupKey.getKey(dataId, group));
                    LOGGER.info("[{}] [polling-resp] config changed. dataId={}, group={}", agent.getName(), dataId,
                            group);
                } else if (keyArr.length == 3) {
                    String tenant = keyArr[2];
                    updateList.add(GroupKey.getKeyTenant(dataId, group, tenant));
                    LOGGER.info("[{}] [polling-resp] config changed. dataId={}, group={}, tenant={}", agent.getName(),
                            dataId, group, tenant);
                } else {
                    LOGGER.error("[{}] [polling-resp] invalid dataIdAndGroup error {}", agent.getName(),
                            dataIdAndGroup);
                }
            }
        }
        return updateList;
    }

    */
/**
     * 核心代码
     *//*

    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    public ClientWorker1(final HttpAgent agent, final ConfigFilterChainManager configFilterChainManager,
                         final Properties properties) {
        this.agent = agent;
        this.configFilterChainManager = configFilterChainManager;

        // 初始化长轮询超时时间等参数
        init(properties);

        // 初始化一个定时调度的线程池. 用于检测配置文件总数是否变化, 每多出3000个配置文件会触发一个新的线程(由executorService线程池创建)去处理
        this.executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("com.alibaba.nacos.client.Worker." + agent.getName());
                t.setDaemon(true);
                return t;
            }
        });

        // 再初始化一个定时调度的线程池. 为每3000个配置文件开启一个长轮询的线程 去检测配置文件是否发生变化及处理
        this.executorService = Executors
                .newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("com.alibaba.nacos.client.Worker.longPolling." + agent.getName());
                        t.setDaemon(true);
                        return t;
                    }
                });

        // 使用executor执行一个10ms间隔的定时任务, 检测配置文件总数是否变化
        this.executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    // 核心代码
                    checkConfigInfo();
                } catch (Throwable e) {
                    LOGGER.error("[" + agent.getName() + "] [sub-check] rotate check error", e);
                }
            }
        }, 1L, 10L, TimeUnit.MILLISECONDS);
    }

    private void init(Properties properties) {

        timeout = Math.max(ConvertUtils.toInt(properties.getProperty(PropertyKeyConst.CONFIG_LONG_POLL_TIMEOUT),
                Constants.CONFIG_LONG_POLL_TIMEOUT), Constants.MIN_CONFIG_LONG_POLL_TIMEOUT);

        taskPenaltyTime = ConvertUtils
                .toInt(properties.getProperty(PropertyKeyConst.CONFIG_RETRY_TIME), Constants.CONFIG_RETRY_TIME);

        this.enableRemoteSyncConfig = Boolean
                .parseBoolean(properties.getProperty(PropertyKeyConst.ENABLE_REMOTE_SYNC_CONFIG));
    }

    @Override
    public void shutdown() throws NacosException {
        String className = this.getClass().getName();
        LOGGER.info("{} do shutdown begin", className);
        ThreadUtils.shutdownThreadPool(executorService, LOGGER);
        ThreadUtils.shutdownThreadPool(executor, LOGGER);
        LOGGER.info("{} do shutdown stop", className);
    }

    class LongPollingRunnable implements Runnable {

        private final int taskId;

        public LongPollingRunnable(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {

            List<CacheData> cacheDatas = new ArrayList<CacheData>();
            List<String> inInitializingCacheList = new ArrayList<String>();
            try {
                // 检查本地配置 (同步本地配置信息到缓存, 本地配置变化则通知监听器)
                for (CacheData cacheData : cacheMap.values()) {
                    // 只处理当前分片的配置
                    if (cacheData.getTaskId() == taskId) {
                        // 收录属于当前分片的配置, 以便检查服务端配置时使用
                        cacheDatas.add(cacheData);
                        try {
                            // 同步本地配置信息 到 缓存
                            checkLocalConfig(cacheData);
                            // 本地配置存在时, 若已变化则通知监听器
                            if (cacheData.isUseLocalConfigInfo()) {
                                // 找出内容md5不是最新的监听器, 回调监听器的方法
                                cacheData.checkListenerMd5();
                            }
                        } catch (Exception e) {
                            LOGGER.error("get local config info error", e);
                        }
                    }
                }

                // 检查服务端配置, 获取已变更的配置文件列表
                List<String> changedGroupKeys = checkUpdateDataIds(cacheDatas, inInitializingCacheList);
                if (!CollectionUtils.isEmpty(changedGroupKeys)) {
                    LOGGER.info("get changedGroupKeys:" + changedGroupKeys);
                }

                // 遍历处理已变更的配置文件, 更新缓存中的信息
                for (String groupKey : changedGroupKeys) {
                    String[] key = GroupKey.parseKey(groupKey);
                    String dataId = key[0];
                    String group = key[1];
                    String tenant = null;
                    if (key.length == 3) {
                        tenant = key[2];
                    }
                    try {
                        // 请求服务端接口 /v1/cs/configs, 获取配置文件信息(存在时会刷新本地快照)
                        String[] ct = getServerConfig(dataId, group, tenant, 3000L);
                        CacheData cache = cacheMap.get(GroupKey.getKeyTenant(dataId, group, tenant));
                        // 更新缓存中的配置文件内容和文件类型
                        cache.setContent(ct[0]);
                        if (null != ct[1]) {
                            cache.setType(ct[1]);
                        }
                        LOGGER.info("[{}] [data-received] dataId={}, group={}, tenant={}, md5={}, content={}, type={}", agent.getName(), dataId, group, tenant, cache.getMd5(), ContentUtils.truncateContent(ct[0]), ct[1]);
                    } catch (NacosException ioe) {
                        String message = String.format("[%s] [get-update] get changed config exception. dataId=%s, group=%s, tenant=%s", agent.getName(), dataId, group, tenant);
                        LOGGER.error(message, ioe);
                    }
                }

                for (CacheData cacheData : cacheDatas) {
                    // 若配置文件为已初始化 或 没使用本地配置且初始化中, 则通知监听器 并设置初始化为结束
                    if (!cacheData.isInitializing() || inInitializingCacheList
                            .contains(GroupKey.getKeyTenant(cacheData.dataId, cacheData.group, cacheData.tenant))) {
                        cacheData.checkListenerMd5();
                        cacheData.setInitializing(false);
                    }
                }
                inInitializingCacheList.clear();

                // 继续轮询执行
                executorService.execute(this);
            } catch (Throwable e) {
                // 如果轮询任务异常，将在下次任务延迟执行(默认为2s)
                LOGGER.error("longPolling error : ", e);
                executorService.schedule(this, taskPenaltyTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    public boolean isHealthServer() {
        return isHealthServer;
    }

    private void setHealthServer(boolean isHealthServer) {
        this.isHealthServer = isHealthServer;
    }

    final ScheduledExecutorService executor;

    final ScheduledExecutorService executorService;

    */
/**
     * groupKey -> cacheData.
     *//*

    private final ConcurrentHashMap<String, CacheData> cacheMap = new ConcurrentHashMap<String, CacheData>();

    private final HttpAgent agent;

    private final ConfigFilterChainManager configFilterChainManager;

    private boolean isHealthServer = true;

    private long timeout;

    private double currentLongingTaskCount = 0;

    private int taskPenaltyTime;

    private boolean enableRemoteSyncConfig = false;
}
*/
