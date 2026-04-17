package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.mx.nqboard.common.core.constant.DriverConstants;
import com.mx.nqboard.common.core.exception.ReadPointException;
import com.mx.nqboard.common.core.exception.WritePointException;
import com.mx.nqboard.device.api.enums.PointTypeFlagEnum;
import com.mx.nqboard.device.service.DriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * opc-ua驱动
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
public class DriverCustomOpcUaServiceImpl implements DriverCustomService {
    private final Map<String, OpcUaClient> clientMap = new ConcurrentHashMap<>(16);

    @Override
    public String read(Map<String, Object> driverInfo, Map<String, Object> pointInfo) {
        return readValue(getClient(driverInfo), pointInfo);
    }

    @Override
    public Boolean write(Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value) {
        return writeValue(getClient(driverInfo), pointInfo, value);
    }

    @Override
    public String getStatus() {
        return DriverConstants.OPC_UA;
    }


    /**
     * 获取 opc ua客户端
     *
     * @param driverInfo 驱动信息
     * @return OpcUaClient
     */
    private synchronized OpcUaClient getClient(Map<String, Object> driverInfo) {
        log.debug("Opc Ua Server Connection Info {}", driverInfo);
        String host = String.valueOf(driverInfo.get("ip"));
        String port = String.valueOf(driverInfo.get("port"));
        String path = String.valueOf(driverInfo.get("path"));
        String deviceId = String.format("%s:%s", host, port);

        OpcUaClient opcUaClient = clientMap.get(deviceId);
        if (ObjectUtil.isNull(opcUaClient)) {
            try {
                opcUaClient = OpcUaClient.create(String.format("opc.tcp://%s:%s%s", host, port, path),
                        endpoints -> endpoints.stream().findFirst(),
                        configBuild -> configBuild.setIdentityProvider(new AnonymousProvider()).setRequestTimeout(UInteger.valueOf(5000)).build());

                clientMap.put(deviceId, opcUaClient);
            } catch (Exception e) {
                log.error("get opc ua client error: {}", e.getMessage());
                clientMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                throw new RuntimeException(e.getMessage());
            }
        }
        return opcUaClient;
    }

    /**
     * 获取 Value
     *
     * @param client    client
     * @param pointInfo Point Attribute Config
     * @return value
     */
    private String readValue(OpcUaClient client, Map<String, Object> pointInfo) {
        try {
            NodeId nodeId = getNode(pointInfo);
            CompletableFuture<String> value = new CompletableFuture<>();
            client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> value.complete(dataValue.getValue().getValue().toString()));
            return value.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("read opc ua value error: {}", e.getMessage());
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * 写入 OpcUa 值
     *
     * @param client    client
     * @param pointInfo 位号信息
     * @param value     写入值
     */
    private boolean writeValue(OpcUaClient client, Map<String, Object> pointInfo, Object value) {
        try {
            NodeId nodeId = getNode(pointInfo);
            client.connect().get();
            return writeNode(client, nodeId, String.valueOf(pointInfo.get("dataType")), value);
        } catch (Exception e) {
            log.error("Write opc ua value error: {}", e.getMessage());
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * 获取 Opc Ua Item
     *
     * @param pointInfo 位号信息
     * @return OpcUa Node
     */
    private NodeId getNode(Map<String, Object> pointInfo) {
        int namespace = Integer.parseInt(String.valueOf(pointInfo.get("namespace")));
        String tag = String.valueOf(pointInfo.get("tag"));
        return new NodeId(namespace, tag);
    }

    /**
     * Write Opc Ua Node
     *
     * @param client OpcUaClient
     * @param nodeId OpcUa Node
     * @param wValue 写入值
     * @return 是否写入
     * @throws ExecutionException   ExecutionException
     * @throws InterruptedException InterruptedException
     */
    private boolean writeNode(OpcUaClient client, NodeId nodeId, String dataType, Object wValue) throws ExecutionException, InterruptedException {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(dataType);
        if (Objects.isNull(valueType)) {
            throw new RuntimeException("Unsupported type of " + dataType);
        }

        CompletableFuture<StatusCode> status = new CompletableFuture<>();
        switch (valueType) {
            case INT:
                int intValue = Integer.parseInt(String.valueOf(wValue));
                status = client.writeValue(nodeId, new DataValue(new Variant(intValue)));
                break;
            case LONG:
                long longValue = Long.parseLong(String.valueOf(wValue));
                status = client.writeValue(nodeId, new DataValue(new Variant(longValue)));
                break;
            case FLOAT:
                float floatValue = Float.parseFloat(String.valueOf(wValue));
                status = client.writeValue(nodeId, new DataValue(new Variant(floatValue)));
                break;
            case DOUBLE:
                double doubleValue = Double.parseDouble(String.valueOf(wValue));
                status = client.writeValue(nodeId, new DataValue(new Variant(doubleValue)));
                break;
            case BOOLEAN:
                boolean booleanValue = Boolean.parseBoolean(String.valueOf(wValue));
                status = client.writeValue(nodeId, new DataValue(new Variant(booleanValue)));
                break;
            case STRING:
                status = client.writeValue(nodeId, new DataValue(new Variant(String.valueOf(wValue))));
                break;
            default:
                break;
        }

        if (Objects.nonNull(status) && Objects.nonNull(status.get())) {
            return status.get().getValue() > 0;
        }
        return false;
    }
}
