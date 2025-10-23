package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.ImageUrl;
import org.example.ai_api.Bean.Model.ContentItem.ImageUrlContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.example.ai_api.Bean.WebRequest.ImageRecognition;
import org.example.ai_api.Persistence.Repository.FileUploadInfoRepository;
import org.example.ai_api.Utils.MinioOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 将用户输入封装为message结构
 * @author 10353965
 */
@Component
public class ImgMessageCreator {

    private static final String DEFAULT_CONTENT = "帮我对比这两张图片";

    @Autowired
    private FileUploadInfoRepository fileUploadInfoRepository;
    @Autowired
    private MinioOperations minioOperations;

    /**
     * 将图片识别请求中新的文本与图片封装为message结构
     * @param imageRecognition 图片识别请求
     * @return 封装后的message结构
     */
    public AIChatMessage createImgMessage(ImageRecognition imageRecognition) throws Exception {
        AIChatMessage message = new AIChatMessage();
        message.setRole("user");
        List<BaseContentItem> contentList = new ArrayList<>();
        // 文本内容
        String text = StringUtils.hasText(imageRecognition.getContent())
                ? imageRecognition.getContent()
                : DEFAULT_CONTENT;
        contentList.add(new TextContentItem(text));
        // 图片内容
        if (imageRecognition.getImages() != null) {
            for (String imgId : imageRecognition.getImages()) {
                FileUpload fileUpload = fileUploadInfoRepository.findById(imgId).orElseThrow(() -> new RuntimeException("不存在对应id的文件"));
                ImageUrlContentItem imgUrl = new ImageUrlContentItem();
                String base64 = minioOperations.getFileBase64(fileUpload.getFilePath(), fileUpload.getFileType());
                ImageUrl url = new ImageUrl();
                url.setUrl(base64);
                imgUrl.setImageUrl(url);
                contentList.add(imgUrl);
            }
        }
        message.setContent(contentList);
        return message;
    }

}
