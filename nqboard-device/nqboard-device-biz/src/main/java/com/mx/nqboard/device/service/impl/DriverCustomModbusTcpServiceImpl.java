package com.mx.nqboard.device.service.impl;

import com.mx.nqboard.common.core.constant.DriverConstants;
import com.mx.nqboard.common.core.exception.ReadPointException;
import com.mx.nqboard.common.core.exception.WritePointException;
import com.mx.nqboard.device.api.enums.PointTypeFlagEnum;
import com.mx.nqboard.device.service.DriverCustomService;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * modbus-tcp驱动
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
public class DriverCustomModbusTcpServiceImpl implements DriverCustomService {
    private final Map<String, ModbusMaster> connectMap = new ConcurrentHashMap<>(16);
    protected static ModbusFactory modbusFactory = new ModbusFactory();

    @Override
    public String read(Map<String, Object> driverInfo, Map<String, Object> pointInfo) {
        return readValue(getConnector(driverInfo), pointInfo);
    }

    @Override
    public Boolean write(Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value) {
        return writeValue(getConnector(driverInfo), pointInfo, value);
    }

    @Override
    public String getStatus() {
        return DriverConstants.MODBUS_TCP;
    }

    /**
     * 获取 Modbus Master
     *
     * @param driverInfo 驱动信息
     * @return ModbusMaster
     */
    private synchronized ModbusMaster getConnector(Map<String, Object> driverInfo) {
        log.debug("Modbus Tcp Connection Info: {}", driverInfo);
        String host = String.valueOf(driverInfo.get("ip"));
        String port = String.valueOf(driverInfo.get("port"));
        String deviceId = String.format("%s:%s", host, port);
        ModbusMaster modbusMaster = connectMap.get(deviceId);
        if (ObjectUtils.isEmpty(modbusMaster)) {
            IpParameters params = new IpParameters();
            params.setHost(host);
            params.setPort(Integer.parseInt(port));
            modbusMaster = modbusFactory.createTcpMaster(params, true);
            try {
                modbusMaster.init();
                connectMap.put(deviceId, modbusMaster);
            } catch (ModbusInitException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Connect modbus master error: {}", e.getMessage(), e);
                throw new RuntimeException(e.getMessage());
            }
        }
        return modbusMaster;
    }

    /**
     * 获取 Value
     *
     * @param modbusMaster ModbusMaster
     * @param pointInfo    位号属性配置
     * @return R of String Value
     */
    private String readValue(ModbusMaster modbusMaster, Map<String, Object> pointInfo) {
        int slaveId = Integer.parseInt(String.valueOf(pointInfo.get("slaveId")));
        int functionCode = Integer.parseInt(String.valueOf(pointInfo.get("functionCode")));
        int offset = Integer.parseInt(String.valueOf(pointInfo.get("offset")));
        String dataType = String.valueOf(pointInfo.get("dataType"));

        switch (functionCode) {
            case 1:
                BaseLocator<Boolean> coilLocator = BaseLocator.coilStatus(slaveId, offset);
                Boolean coilValue = getMasterValue(modbusMaster, coilLocator);
                return String.valueOf(coilValue);
            case 2:
                BaseLocator<Boolean> inputLocator = BaseLocator.inputStatus(slaveId, offset);
                Boolean inputStatusValue = getMasterValue(modbusMaster, inputLocator);
                return String.valueOf(inputStatusValue);
            case 3:
                BaseLocator<Number> holdingLocator = BaseLocator.holdingRegister(slaveId, offset, getValueType(dataType));
                Number holdingValue = getMasterValue(modbusMaster, holdingLocator);
                return String.valueOf(holdingValue);
            case 4:
                BaseLocator<Number> inputRegister = BaseLocator.inputRegister(slaveId, offset, getValueType(dataType));
                Number inputRegisterValue = getMasterValue(modbusMaster, inputRegister);
                return String.valueOf(inputRegisterValue);
            default:
                return "0";
        }
    }


    /**
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param pointInfo    位号属性配置
     * @param value        value
     * @return Write Result
     */
    private boolean writeValue(ModbusMaster modbusMaster, Map<String, Object> pointInfo, Object value) {
        int functionCode = Integer.parseInt(String.valueOf(pointInfo.get("functionCode")));
        int slaveId = Integer.parseInt(String.valueOf(pointInfo.get("slaveId")));
        int offset = Integer.parseInt(String.valueOf(pointInfo.get("offset")));
        String dataType = String.valueOf(pointInfo.get("dataType"));

        switch (functionCode) {
            case 1:
                WriteCoilResponse coilResponse = setMasterValue(modbusMaster, slaveId, offset, Boolean.valueOf(String.valueOf(value)));
                return !coilResponse.isException();
            case 3:
                BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, getValueType(dataType));
                setMasterValue(modbusMaster, locator, value);
                return true;
            default:
                return false;
        }
    }


    /**
     * 获取 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param locator      BaseLocator
     * @param <T>          类型
     * @return 类型
     */
    private <T> T getMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator) {
        try {
            return modbusMaster.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Read modbus master value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param slaveId      从站ID
     * @param offset       偏移量
     * @param value        写入值
     * @return WriteCoilResponse
     */
    private WriteCoilResponse setMasterValue(ModbusMaster modbusMaster, int slaveId, int offset, Boolean value) {
        try {
            WriteCoilRequest coilRequest = new WriteCoilRequest(slaveId, offset, value);
            return (WriteCoilResponse) modbusMaster.send(coilRequest);
        } catch (ModbusTransportException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * 写入 ModbusMaster 值
     *
     * @param modbusMaster ModbusMaster
     * @param locator      BaseLocator
     * @param value        写入值
     * @param <T>          类型
     */
    private <T> void setMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator, Object value) {
        try {
            modbusMaster.setValue(locator, value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }


    /**
     * 获取数据类型
     *
     * @param type Value Type
     * @return Modbus Data Type
     */
    private int getValueType(String type) {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(type);
        return switch (valueType) {
            case LONG -> DataType.FOUR_BYTE_INT_SIGNED;
            case FLOAT -> DataType.FOUR_BYTE_FLOAT;
            case DOUBLE -> DataType.EIGHT_BYTE_FLOAT;
            default -> DataType.TWO_BYTE_INT_SIGNED;
        };
    }
}
