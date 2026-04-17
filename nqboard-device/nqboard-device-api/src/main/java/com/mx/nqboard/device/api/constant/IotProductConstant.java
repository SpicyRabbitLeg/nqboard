package com.mx.nqboard.device.api.constant;

/**
 * @author SpicyRabbitLeg
 */
public interface IotProductConstant {

    /**
     * 保存产品topic
     */
    String PRODUCT_SAVE_TOPIC = "product_save_topic";

    /**
     * IOT采集
     */
    String COLLECTION_TOPIC = "collection_topic";

    /**
     * IOT采集消费端
     */
    String COLLECTION_GROUP = "collection_group";

    /**
     * 唯一标识传入错误
     */
    String ERROR_PRODUCT = "iot.product.identifier";

    /**
     * 产品保存错误
     */
    String ERROR_PRODUCT_SAVE = "iot.product.save";

    /**
     * word传入错误
     */
    String ERROR_WORD = "iot.word";

    /**
     * 字段长度限制1
     */
    String ERROR_VALIDATOR_LENGTH = "column.validator.length";

    /**
     * 字段长度限制2
     */
    String ERROR_VALIDATOR_LENGTH2 = "column.validator.length2";

    /**
     * 字段名只能包含字母、数字和下划线或中文字符
     */
    String ERROR_VALIDATOR_ZW ="column.validator.zw";

    /**
     * 字段名不能是数据库的保留关键字
     */
    String ERROR_VALIDATOR_KEYWORDS = "column.validator.keyWords";

    /**
     * 字段名应该使用蛇形命名法（小写字母和下划线）
     */
    String ERROR_VALIDATOR_SERPENTINE = "column.validator.serpentine";
}
