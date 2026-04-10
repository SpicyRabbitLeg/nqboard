package com.mx.nqboard.device.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * 转中文
 *
 * @author 泥鳅压滑板
 */
public class ChineseUtils {
    /**
     * 英文校验
     */
    private static final String REGEX = "^[a-zA-Z]+$";

    /**
     * 传入中文返回拼音，如果传入的是英文即可返回
     *
     * @param word word
     * @return 拼音
     */
    public static String getWord(String word) {
        Assert.isTrue(StrUtil.isNotBlank(word), MsgUtils.getMessage(IotProductConstant.ERROR_WORD));
        if (word.matches(REGEX)) {
            return word;
        }
        return PinyinUtil.getPinyin(word, StringConstants.File.UNDERLINE).replaceAll(" ","").toLowerCase(Locale.ROOT);
    }
}
