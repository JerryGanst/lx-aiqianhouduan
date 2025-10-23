package org.example.ai_api.Strategy.Converter;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件格式转换策略模式接口.
 * @author 10353965
 */
public interface FileConvertStrategy {
    /**
     *  文件格式转换
     * @param file  待转换文件
     * @return  转换后的文件
     * @throws Exception  转换过程中可能出现的异常
     */
    byte[] execute(MultipartFile file) throws Exception;

    /**
     *  判断是否支持该格式转换
     * @param format  待转换文件格式
     * @return  是否支持该格式转换
     */
    boolean supports(String format);
}
