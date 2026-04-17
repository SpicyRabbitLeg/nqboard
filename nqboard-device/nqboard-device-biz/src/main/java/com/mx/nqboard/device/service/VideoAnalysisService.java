package com.mx.nqboard.device.service;

import com.mx.nqboard.device.api.dto.*;
import com.mx.nqboard.device.api.vo.VideoAnalysisResponseVO;

/**
 * 摄像头AI服务接口
 *
 * @author SpicyRabbitLeg
 */
public interface VideoAnalysisService {

	/**
	 * 注册
	 * @param videoAnalysisDto videoAnalysisDto
	 * @return res
	 */
	VideoAnalysisResponseVO register(VideoAnalysisDTO videoAnalysisDto);

	/**
	 * 更新
	 * @param videoAnalysisDto videoAnalysisDto
	 * @return res
	 */
	VideoAnalysisResponseVO update(VideoAnalysisDTO videoAnalysisDto);

	/**
	 * 删除
	 * @param videoAnalysisDto videoAnalysisDto
	 * @return res
	 */
	VideoAnalysisResponseVO delete(VideoAnalysisDTO videoAnalysisDto);

	/**
	 * 修改状态
	 * @param videoAnalysisDto videoAnalysisDto
	 * @return res
	 */
	VideoAnalysisResponseVO controlStatus(VideoAnalysisDTO videoAnalysisDto);

	/**
	 * 校验在线状态
	 * @param videoAnalysisDto videoAnalysisDto
	 * @return res
	 */
	VideoAnalysisResponseVO checkOnline(VideoAnalysisDTO videoAnalysisDto);
}
