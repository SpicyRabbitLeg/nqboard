package com.mx.nqboard.device.test;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.SecureRandom;

public class RtspTest {

	/**
	 * 测试网络是否连接通过
	 *
	 * @throws Exception
	 */
	@Test
	public void testUrlConn() throws Exception{
		// 在这里修改你的地址和端口
		String host = "127.0.0.1";
		int port = 9007;

		try (Socket socket = new Socket()) {
			// 连接超时时间：3000 毫秒 = 3 秒
			socket.connect(new InetSocketAddress(host, port), 3000);
			System.out.println("✅ 连接成功：" + host + ":" + port);
		} catch (Exception e) {
			System.out.println("❌ 连接失败：" + host + ":" + port +" message: " + e.getMessage());
		}
	}
}
