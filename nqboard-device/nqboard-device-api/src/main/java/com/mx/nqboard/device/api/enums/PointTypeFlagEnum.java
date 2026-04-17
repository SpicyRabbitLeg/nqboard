package com.mx.nqboard.device.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author SpicyRabbitLeg
 */
public enum PointTypeFlagEnum {
    /**
     * 类型，描述
     */
    STRING((byte) 0, "string", "字符串"),
    BYTE((byte) 1, "byte", "字节"),
    SHORT((byte) 2, "short", "短整数"),
    INT((byte) 3, "int", "整数"),
    LONG((byte) 4, "long", "长整数"),
    FLOAT((byte) 5, "float", "浮点数"),
    DOUBLE((byte) 6, "double", "双精度浮点数"),
    BOOLEAN((byte) 7, "boolean", "布尔量");

    @EnumValue
    private final Byte index;
    private final String code;
    private final String remark;

    public static PointTypeFlagEnum ofIndex(Byte index) {
        Optional<PointTypeFlagEnum> any = Arrays.stream(values()).filter((type) -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    public static PointTypeFlagEnum ofCode(String code) {
        Optional<PointTypeFlagEnum> any = Arrays.stream(values()).filter((type) -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    public static PointTypeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    public Byte getIndex() {
        return this.index;
    }

    public String getCode() {
        return this.code;
    }

    public String getRemark() {
        return this.remark;
    }

    PointTypeFlagEnum(final Byte index, final String code, final String remark) {
        this.index = index;
        this.code = code;
        this.remark = remark;
    }
}
