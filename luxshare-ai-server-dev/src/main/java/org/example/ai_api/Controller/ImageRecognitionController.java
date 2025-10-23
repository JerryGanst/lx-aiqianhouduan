package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.ImgRecognition;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Service.ImageRecognitionService;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图片识别相关接口
 *
 * @author 10353965
 */
@RestController
@RequestMapping("/imageRecognition")
public class ImageRecognitionController {
    @Autowired
    private ImageRecognitionService imageRecognitionService;

    /**
     * 根据用户id获取图片识别记录
     * @param userId 用户id
     * @return 图片识别记录
     */
    @PostMapping("/getImageRecognitionsByUserId")
    public ResultData<List<Map>> getImgRecognitionsByUserId(
            @RequestParam("userId") String userId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword
    ) {
        List<Map> result = imageRecognitionService.getImageRecognitionsByUserId(userId,keyword);
        Utils.convertMongoIdToStringId(result,"imgRecognitionId", true);
        return ResultData.success("获取成功", result);
    }

    /**
     * 保存用户图片识别记录
     * @param imgRecognition 图片识别结果
     * @return 保存结果
     */
    @PostMapping("/saveImgRecognition")
    public ResultData<ImgRecognition> saveImgRecognition(@RequestBody ImgRecognition imgRecognition) {
        ImgRecognition result = imageRecognitionService.save(imgRecognition);
        return ResultData.success("保存成功", result);
    }

    /**
     * 根据id获取图片识别记录
     * @param id 图片识别记录id
     * @return 图片识别记录
     */
    @PostMapping("/getImgRecognitionById")
    public ResultData<ImgRecognition> getImageRecognitionById(@RequestParam("id") String id) throws Exception {
        ImgRecognition result = imageRecognitionService.getImageRecognitionById(id);
        if (result == null) {
            throw new NotFoundException("不存在对应id的图片识别记录");
        }
        //刷新message中的所有图片链接
        imageRecognitionService.updateImgUrl(result);
        return ResultData.success("获取成功", result);
    }

    /**
     * 根据id删除图片识别记录
     * @param id 图片识别记录id
     * @return 删除结果
     */
    @PostMapping("/deleteImgRecognitionById")
    public ResultData<String> deleteImgRecognitionById(@RequestParam("id") String id) {
        imageRecognitionService.deleteImageRecognitionById(id);
        return ResultData.success("删除成功");
    }

    /**
     * 根据图片id获取图片链接
     * @param id 图片id
     * @return 图片链接
     * @throws Exception 图片不存在
     */
    @PostMapping("/getImgUrlById")
    public ResultData<String> getImgUrlById(@RequestParam("id") String id) throws Exception {
        if (id == null) {
            throw new BadRequestException("id不可为空");
        }
        String result = imageRecognitionService.getImgUrlById(id);
        return ResultData.success("获取成功",result);
    }

    /**
     * 根据id修改图片识别记录标题
     * @param id 图片识别记录id
     * @param title 标题
     * @return 修改结果
     */
    @PostMapping("/changeImageRecognitionTitle")
    public ResultData<String> changeImageRecognitionTitle(@RequestParam("id") String id, @RequestParam("title") String title) {
        if (id == null) {
            throw new BadRequestException("id不可为空");
        }
        if (title == null) {
            throw new BadRequestException("title不可为空");
        }
        imageRecognitionService.changeImageRecognitionTitle(id, title);
        return ResultData.success("修改成功");
    }
}
