package com.mx.nqboard.common.core.constant;

/**
 * @author SpicyRabbitLeg
 */
public interface StringConstants {
    /**
     *
     * 数据类型
     */
    interface Type {
        String HEX = "hex";
        String BYTE = "byte";
        String SHORT = "short";
        String INT = "int";
        String LONG = "long";
        String FLOAT = "float";
        String DOUBLE = "double";
        String BOOLEAN = "boolean";
        String STRING = "string";
        String TIMESTAMP = "timestamp";
    }

    interface Prefix {
        /**
         * 登录用户 redis key
         */
        String LOGIN_TOKEN_KEY = "login_tokens:";

        /**
         * 令牌前缀
         */
        String TOKEN_PREFIX = "Bearer ";

        /**
         * 用户key
         */
        String LOGIN_USER_KEY = "login_user_key";

        /**
         * 验证码 redis key
         */
        String CAPTCHA_KEY = "captcha_key:";

        /**
         * app key
         */
        String APP_KEY = "appkey";

        /**
         * app secret
         */
        String APP_SECRET = "appsecret";
    }


    interface Request {
        /**
         * 请求成功
         */
        String SUCCESS = "request success";
        /**
         * 请求失败
         */
        String ERROR = "request error";
    }


    interface File {
        /**
         * 路径斜杠
         */
        String SLASH = "/";


        /**
         * 逗号
         */
        String COMMA = ",";

        /**
         * 下划线
         */
        String UNDERLINE = "_";

        /**
         * 冒号
         */
        String COLON = ":";
    }

    interface Switch {
        /**
         * 开
         */
        String ENABLE = "1";

        /**
         * 关
         */
        String DISABLE = "0";
    }


    interface NumberStr {
        /**
         * 零
         */
        String ZERO = "0";
    }


    interface Ip {
        /**
         * 本级ip
         */
        String LOCALHOST = "127.0.0.1";

        /**
         * 未知
         */
        String UNKNOWN = "unknown";
    }


    interface Web {
        /**
         * 请求开始时间
         */
        String REQUEST_START_TIME = "REQUEST-START-TIME";
    }

    interface Other {
        String NULL = "null";

        /**
         * 空
         */
        String EMPTY = "";
    }
}
