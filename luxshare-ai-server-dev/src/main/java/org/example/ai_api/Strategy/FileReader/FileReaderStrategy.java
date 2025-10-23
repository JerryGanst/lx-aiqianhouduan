package org.example.ai_api.Strategy.FileReader;

import java.io.InputStream;

/**
 * 文件文本读取策略接口
 * @author 10353965
 */
public interface FileReaderStrategy {
    /**
     *  读取文件文本
     * @param inputStream  文件输入流
     * @return  文件文本
     * @throws Exception  读取过程中可能出现的异常
     */
    String read(InputStream inputStream) throws Exception;

    /**
     *   判断是否支持该格式读取
     * @param fileName    待读取文件名
     * @return    是否支持该格式读取
     */
    Boolean support(String fileName);
}
