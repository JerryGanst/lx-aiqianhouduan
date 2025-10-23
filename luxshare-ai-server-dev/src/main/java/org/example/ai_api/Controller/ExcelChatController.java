package org.example.ai_api.Controller;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.Entity.ExcelChatCompletionRecord;
import org.example.ai_api.Bean.Entity.ExcelChatInfo;
import org.example.ai_api.Bean.WebRequest.ExcelChatCompletionSave;
import org.example.ai_api.Service.ExcelChatService;
import org.example.ai_api.Bean.Model.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Excel 会话相关接口。
 * @author 10353965
 */
@RestController
@RequestMapping("/excelChat")
@RequiredArgsConstructor
public class ExcelChatController {
	@Autowired
	private ExcelChatService excelChatService;

	/**
	 * 根据id是否存在，保存或更新 Excel 会话。
	 *
	 * @param info Excel 会话信息
	 * @return 包含已保存会话信息的结果
	 */
	@PostMapping("/saveExcelChat")
	public ResultData<ExcelChatInfo> save(@RequestBody ExcelChatInfo info) {
		return ResultData.success(excelChatService.saveChat(info));
	}

	/**
	 * 根据主键 ID 获取会话详情。
	 *
	 * @param id 会话主键 ID
	 * @return 成功返回会话信息；不存在时返回失败结果
	 */
	@PostMapping("/getExcelChatById")
	public ResultData<ExcelChatInfo> getById(@RequestParam("id") String id) {
		return excelChatService.getById(id)
			.map(excelChatInfo -> {
				excelChatService.updateDownloadUrls(excelChatInfo.getMessages());
				return ResultData.success(excelChatInfo);
			})
			.orElse(ResultData.fail("记录不存在"));
	}

	/**
	 * 根据用户id查询excel会话列表
	 * @param userId 用户id
	 * @param keyword 关键词
	 * @return 会话列表（可能为空）
	 */
	@PostMapping("/getExcelChatByUserId")
	public ResultData<List<Map>> getByUserId(
			@RequestParam("userId") String userId,
			@RequestParam(value = "keyword",defaultValue = "") String keyword
	) {
		return ResultData.success(excelChatService.getByUserIdAndKeyWord(userId,keyword));
	}

	/**
	 * 根据主键 ID 更新会话标题。
	 *
	 * @param id 会话主键 ID
	 * @param title 要更新的会话标题
	 * @return 成功返回更新后的会话标题；不存在时返回失败结果
	 */
	@PostMapping("/updateExcelChatTitleById")
	public ResultData<String> update(@RequestParam("id") String id, @RequestParam("title") String title)  {
		excelChatService.updateTitleById(id, title);
		return ResultData.success("更新成功");
	}

	/**
	 * 根据主键 ID 删除会话。
	 *
	 * @param id 会话主键 ID
	 * @return 删除结果，成功时消息为“删除成功”，data 为 null
	 */
	@PostMapping("/deleteExcelChatById")
	public ResultData<Void> delete(@RequestParam("id") String id) {
		excelChatService.delete(id);
		return ResultData.success("删除成功", null);
	}

	/**
	 * 保存会话的最后一次补全记录。
	 *
	 * @param info 补全保存参数
	 * @return 保存后的补全记录
	 */
	@PostMapping("/saveLastInfo")
	public ResultData<ExcelChatCompletionRecord> saveLastInfo(@RequestBody ExcelChatCompletionSave info) {
		return ResultData.success(excelChatService.saveLastInfo(info));
	}
}
