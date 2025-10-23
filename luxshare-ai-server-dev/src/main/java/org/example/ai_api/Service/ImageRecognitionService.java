package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Entity.ImgRecognition;
import org.example.ai_api.Bean.Model.*;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.ImageRecognitionDao;
import org.example.ai_api.Persistence.Repository.FileUploadInfoRepository;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author 10353965
 */
@Service
public class ImageRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(ImageRecognitionService.class);
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private ImageRecognitionDao imageRecognitionDao;
    @Autowired
    private FileUploadInfoRepository fileUploadInfoRepository;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;

    /**
     * 保存图片识别结果
     *
     * @param imgRecognition 图片识别结果
     * @return 保存结果
     */
    public ImgRecognition save(ImgRecognition imgRecognition) {
        logger.info("保存图片识别结果,{}", imgRecognition.getUserId());
        if (imgRecognition.getId() == null || imgRecognition.getId().isEmpty()) {
            imgRecognition.setId(null);
        }
        if (imgRecognition.getCreateTime() == null) {
            imgRecognition.setCreateTime(Utils.getNowDate());
        }
        imgRecognition.setUpdateTime(Utils.getNowDate());
        imgRecognition.setLastOperationTime(Utils.getNowDate());
        //刷新message中的图片base64链接
        updateImgData(imgRecognition);
        return imageRecognitionDao.save(imgRecognition);
    }

    /**
     * 根据用户id获取图片识别结果
     *
     * @param userId 用户id
     * @return 图片识别结果
     */
    public List<Map> getImageRecognitionsByUserId(String userId, String keyword) {
        logger.info("根据用户id获取图片识别结果,{}", userId);
        return imageRecognitionDao.findImgRecognitionByUserIdWithFields(userId, keyword);
    }

//    /**
//     * 处理图片识别请求，将图片识别请求封装为request
//     *
//     * @param imageRecognition 图片识别请求
//     * @return 封装后的request
//     */
//    public ImageRecognitionRequest processImageRecognitionRequest(ImageRecognition imageRecognition) throws Exception {
//        logger.info("处理图片识别请求");
//        ImageRecognitionRequest imageRecognitionRequest = new ImageRecognitionRequest();
//        //加入用户id
//        imageRecognitionRequest.setUserId(imageRecognition.getUserId());
//        //加入sessionId
//        imageRecognitionRequest.setSessionId(imageRecognition.getSessionId());
//        //将此次新文本与图片封装为message结构
//        UserMessage imgMessage = createImgMessage(imageRecognition);
//        //将新封装的message结构加到记录结尾
//        List<BaseMessage> imgMessages = imageRecognition.getMessages();
//        if (imgMessages == null) {
//            //如果message为空，新建一个空的message列表
//            imgMessages = new ArrayList<>();
//        }
//        imgMessages.add(imgMessage);
//        //新建的message数据添加到request中
//        imageRecognitionRequest.setMessages(imgMessages);
//        imageRecognitionRequest.setStream(true);
//        return imageRecognitionRequest;
//    }

    /**
     * 根据id获取图片识别结果
     *
     * @param id 图片识别结果id
     * @return 图片识别结果
     */
    public ImgRecognition getImageRecognitionById(String id) {
        logger.info("根据id获取图片识别结果,{}", id);
        return imageRecognitionDao.getImgRecognitionById(id);
    }

    /**
     * 根据id删除图片识别结果
     *
     * @param id 图片识别结果id
     */
    public void deleteImageRecognitionById(String id) {
        logger.info("根据id删除图片识别结果,{}", id);
        imageRecognitionDao.deleteImgRecognitionById(id);
    }

    /**
     * 根据id获取图片链接
     * @param id 图片id
     * @return 图片链接
     * @throws Exception 图片不存在
     */
    public String getImgUrlById(String id) throws Exception {
        FileUpload fileUpload = fileUploadInfoRepository.findById(id).orElseThrow(() -> new NotFoundException("不存在对应id的文件"));
        String result = minioOperations.getDownloadUrl(fileUpload.getFilePath(), 3600, null);
        return Utils.exchangeFileUrl(result, local, minioProxy);
    }

    /**
     * 保存前刷新图片base64链接
     * @param imgRecognition 图片识别结果
     * @throws Exception 图片不存在
     */
    public void updateImgUrl(ImgRecognition imgRecognition) throws Exception {
        if (imgRecognition.getImgMessages() == null) {
            return;
        }
        for (BaseMessage message : imgRecognition.getImgMessages()) {
            if (message instanceof UserMessage) {
                //更新用户信息中的图片管理链接
                List<ImgContent> imgContentList = ((UserMessage) message).getContent();
                for (ImgContent imgContent : imgContentList) {
                    if (imgContent instanceof ImageUrl) {
                        FileUpload img = ((ImageUrl) imgContent).getUrl().getImage();
                        String imgUrl = minioOperations.getDownloadUrl(img.getFilePath(),3600,null);
                        img.setFileUrl(Utils.exchangeFileUrl(imgUrl, local, minioProxy));
                    }
                }
            }
        }
    }

    public void changeImageRecognitionTitle(String id,String title){
        ImgRecognition imgRecognition = imageRecognitionDao.getImgRecognitionById(id);
        if (imgRecognition == null) {
            throw new NotFoundException("不存在对应id的图片识别记录");
        }
        imgRecognition.setTitle(title);
        imgRecognition.setUpdateTime(Utils.getNowDate());
        imgRecognition.setLastOperationTime(Utils.getNowDate());
        imageRecognitionDao.save(imgRecognition);
    }

//    /**
//     * 将图片识别请求中新的文本与图片封装为message结构
//     * @param imageRecognition 图片识别请求
//     * @return 封装后的message结构
//     */
//    private UserMessage createImgMessage (ImageRecognition imageRecognition) throws Exception {
//        UserMessage message = new UserMessage();
//        message.setRole("user");
//        List<ImgContent> contentList = new java.util.ArrayList<>();
//        // 文本内容
//        if (imageRecognition.getContent() != null && !imageRecognition.getContent().isEmpty()) {
//            TextContent textContent = new TextContent();
//            textContent.setText(imageRecognition.getContent());
//            contentList.add(textContent);
//        }
//        // 图片内容
//        if (imageRecognition.getImages() != null) {
//            for (String imgId : imageRecognition.getImages()) {
//                FileUpload fileUpload = fileUploadInfoRepository.findById(imgId).orElseThrow(() -> new RuntimeException("不存在对应id的文件"));
//                ImageUrl imgUrl = new ImageUrl();
//                String base64 = minioOperations.getFileBase64(fileUpload.getFilePath(), fileUpload.getFileType());
//                Url url = new Url();
//                url.setUrl(base64);
//                imgUrl.setUrl(url);
//                contentList.add(imgUrl);
//            }
//        }
//        message.setContent(contentList);
//        return message;
//    }

    /**
     * 更新图片数据，将历史记录中的图片ID转换为base64格式的URL
     * @param imgRecognition 图片识别历史记录
     */
    private void updateImgData(ImgRecognition imgRecognition) {
        if (imgRecognition == null || imgRecognition.getImgMessages() == null) {
            return;
        }
        
        for (BaseMessage message : imgRecognition.getImgMessages()) {
            if (message instanceof UserMessage) {
                UserMessage userMessage = (UserMessage) message;
                updateUserMessageImgData(userMessage);
            }
        }
    }
    
    /**
     * 更新用户消息中的图片数据
     * @param userMessage 用户消息
     */
    private void updateUserMessageImgData(UserMessage userMessage) {
        if (userMessage.getContent() == null) {
            return;
        }
        
        for (ImgContent imgContent : userMessage.getContent()) {
            if (imgContent instanceof ImageUrl) {
                ImageUrl imageUrl = (ImageUrl) imgContent;
                if (imageUrl.getUrl() != null && imageUrl.getUrl().getImage() != null) {
                    // 获取图片信息
                    FileUpload imageId = imageUrl.getUrl().getImage();
                    if (imageUrl.getUrl().getUrl() == null||imageUrl.getUrl().getUrl().isEmpty()) {
                        // 根据图片ID获取base64数据
                        String base64Data = getImageBase64ById(imageId);
                        if (base64Data != null) {
                            // 更新URL为base64格式
                            imageUrl.getUrl().setUrl(base64Data);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 根据图片信息获取base64数据
     * @param image 图片信息
     * @return base64数据，如果不存在则返回null
     */
    private String getImageBase64ById(FileUpload image) {
        try {
            return minioOperations.getFileBase64(image.getFilePath(), image.getFileType());
        } catch (Exception e) {
            logger.error("获取图片base64数据失败", e);
            return null;
        }
    }
    
//    /**
//     * 校验单次完整对话记录中图片总数不超过6张
//     * @param messages 消息列表
//     * @throws IllegalArgumentException 如果图片总数超过6张
//     */
//    public void validateImageCountInMessages(List<BaseMessage> messages) {
//        int imageCount = 0;
//        if (messages != null) {
//            for (BaseMessage message : messages) {
//                if (message instanceof UserMessage) {
//                    UserMessage userMessage = (UserMessage) message;
//                    if (userMessage.getContent() != null) {
//                        for (ImgContent imgContent : userMessage.getContent()) {
//                            if (imgContent instanceof ImageUrl) {
//                                imageCount++;
//                                if (imageCount > 6) {
//                                    throw new IllegalArgumentException("单次完整对话记录中图片总数不能超过6张");
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}
