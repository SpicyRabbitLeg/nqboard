package com.mx.nqboard.common.file.local;

import cn.hutool.core.io.FileUtil;
import com.mx.nqboard.common.file.core.FileProperties;
import com.mx.nqboard.common.file.core.FileTemplate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * 本地文件读取模式
 *
 * @author lengleng
 * @date 2022/4/19
 */
@RequiredArgsConstructor
public class LocalFileTemplate implements FileTemplate {

	private final FileProperties properties;

	/**
	 * 创建bucket
	 * @param bucketName bucket名称
	 */
	@Override
	public void createBucket(String bucketName) {
		FileUtil.mkdir(properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName);
	}

	@Override
	public List<? extends Object> getAllBuckets() {
		return null;
	}

	/**
	 * @param bucketName bucket名称
	 * @see <a href= Documentation</a>
	 */
	@Override
	public void removeBucket(String bucketName) {
		FileUtil.del(properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName);
	}

	/**
	 * 上传文件
	 * @param bucketName bucket名称
	 * @param objectName 文件名称
	 * @param stream 文件流
	 * @param contextType 文件类型
	 */
	@Override
	public void putObject(String bucketName, String objectName, InputStream stream, String contextType) {
		// 当 Bucket 不存在时创建
		String dir = properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName;
		if (!FileUtil.isDirectory(properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName)) {
			createBucket(bucketName);
		}

		// 写入文件
		File file = FileUtil.file(dir + FileUtil.FILE_SEPARATOR + objectName);
		FileUtil.writeFromStream(stream, file);
	}

	/**
	 * 获取文件
	 * @param bucketName bucket名称
	 * @param objectName 文件名称
	 * @return 文件输入流
	 */
	@Override
	@SneakyThrows
	public InputStream getObject(String bucketName, String objectName) {
		String dir = properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName;
		return FileUtil.getInputStream(dir + FileUtil.FILE_SEPARATOR + objectName);
	}

	/**
	 * 删除指定存储桶中的对象
	 * @param bucketName 存储桶名称
	 * @param objectName 对象名称
	 * @throws Exception 删除过程中可能抛出的异常
	 */
	@Override
	public void removeObject(String bucketName, String objectName) throws Exception {
		String dir = properties.getLocal().getBasePath() + FileUtil.FILE_SEPARATOR + bucketName;
		FileUtil.del(dir + FileUtil.FILE_SEPARATOR + objectName);
	}

	@Override
	public List<? extends Object> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
		return null;
	}

	/**
	 * 上传文件到指定存储桶
	 * @param bucketName 存储桶名称
	 * @param objectName 文件名称
	 * @param stream 文件输入流
	 * @throws Exception 上传过程中可能发生的异常
	 */
	@Override
	public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
		putObject(bucketName, objectName, stream, null);
	}
}
