package com.mx.nqboard.device.api.constant;

/**
 * 物模型常理
 * @author 泥鳅压滑板
 */
public interface IotModelConstant {
    /**
     * 保存物模型topic
     */
    String MODEL_SAVE_TOPIC = "model_save_topic";

    /**
     * 保存物模型topic
     */
    String MODEL_DEL_TOPIC = "model_del_topic";


    /**
     * 错误唯一标识
     */
    String ERROR_IDENTIFIER = "iot.model.identifier";

    /**
     * 错误唯一标识需存在
     */
    String ERROR_IDENTIFIER_EXIT = "iot.model.identifier.exit";

    /**
     * 错误添加失败
     */
    String ERROR_SAVE = "iot.model.save";

    /**
     * 错误删除失败
     */
    String ERROR_DEL = "iot.model.del";

}
