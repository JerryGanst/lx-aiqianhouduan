package org.example.ai_api.Service;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.Entity.ExcelChatCompletionRecord;
import org.example.ai_api.Bean.Entity.ExcelChatInfo;
import org.example.ai_api.Bean.Model.ExcelChatMessage;
import org.example.ai_api.Bean.WebRequest.ExcelChatCompletionSave;
import org.example.ai_api.Persistence.Dao.ExcelChatDao;
import org.example.ai_api.Persistence.Repository.ExcelChatCompletionRecordRepository;
import org.example.ai_api.Persistence.Repository.ExcelChatInfoRepository;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * excel处理相关服务
 * @author 10353965
 */
@Service
@RequiredArgsConstructor
public class ExcelChatService {
    @Autowired
    private ExcelChatInfoRepository excelChatInfoRepository;
    @Autowired
    private ExcelChatCompletionRecordRepository excelChatCompletionRecordRepository;
    @Autowired
    private ExcelChatDao excelChatDao;
    @Autowired
    private MinioOperations minioOperations;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;

    public ExcelChatInfo saveChat(ExcelChatInfo info) {
        // id为空的请求,在数据库中新建记录
        if (info.getId() == null || info.getId().isEmpty()) {
            info.setId(null);
            info.setCreateTime(Utils.getNowDate());
        }
        info.setUpdateTime(Utils.getNowDate());
        info.setLastOperationTime(Utils.getNowDate());
        return excelChatInfoRepository.save(info);
    }

    public Optional<ExcelChatInfo> getById(String id) {
        return excelChatInfoRepository.findById(id);
    }

    public List<Map> getByUserIdAndKeyWord(String userId, String keyWord) {
        List<Map> result = excelChatDao.findExcelChatByAgentIdWithFields(userId,keyWord);
        Utils.convertMongoIdToStringId(result,"chatId",true);
        return result;
    }

    public void updateTitleById(String id, String title)  {
        excelChatInfoRepository.findById(id).map(existing -> {
            existing.setTitle(title);
            existing.setUpdateTime(Utils.getNowDate());
            existing.setLastOperationTime(Utils.getNowDate());
            return excelChatInfoRepository.save(existing);
        });
    }

    public void delete(String id) {
        excelChatInfoRepository.deleteById(id);
    }

    public ExcelChatCompletionRecord saveLastInfo(ExcelChatCompletionSave info) {
        ExcelChatCompletionRecord record = new ExcelChatCompletionRecord();
        record.setChatId(info.getChatId());
        record.setExcelChatRepeat(info.getExcelChatRepeat());
        record.setCreateTime(Utils.getNowDate());
        return excelChatCompletionRecordRepository.save(record);
    }

    public void updateDownloadUrls(List<ExcelChatMessage> messages){
        messages.forEach(message -> {
            if(message.getObjectName() != null && !message.getObjectName().isEmpty()) {
                try {
                    if(minioOperations.isMinioUrlExpiredOrInvalid(message.getDownloadUrl())){
                        String downloadUrl = minioOperations.getDownloadUrl(message.getObjectName(),3600,null);
                        message.setDownloadUrl(Utils.exchangeFileUrl(downloadUrl,local,minioProxy));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
