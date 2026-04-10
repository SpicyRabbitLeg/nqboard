package com.mx.nqboard.device.service;


import com.mx.nqboard.device.service.impl.TransactionCustomHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 分布式事务handler
 *
 * @author 泥鳅压滑板
 **/
public interface TransactionCustomService<T> {
    /**
     * 执行本地事务
     *
     * @param bizObject 消息内容
     * @return 本地事务是否成功
     */
    boolean executeLocalTransaction(T bizObject);

    /**
     * 事务回查
     *
     * @param bizObject 消息内容
     * @return 本地事务是否成功
     */
    boolean checkLocalTransaction(T bizObject);

    /**
     * 获取业务类型
     *
     * @return 业务类型
     */
    String getStatus();

    /**
     * 将实现类注册到transactionCustomHandler中
     *
     * @param transactionCustomHandler transactionCustomHandler
     */
    @Autowired
    default void selfRegistration(TransactionCustomHandler transactionCustomHandler) {
        transactionCustomHandler.register(getStatus(), this);
    }
}
