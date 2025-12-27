package io.github.lemostic.toolsuite.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ResourceLoader {
    
    /**
     * 从资源路径加载文本内容
     * @param resourcePath 资源路径
     * @return 资源文件的内容，如果加载失败则返回错误信息
     */
    public static String loadResourceFile(String resourcePath) {
        try (InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            if (inputStream == null) {
                return "无法找到资源文件: " + resourcePath;
            }
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            // 移除最后多余的换行符
            if (content.length() > 0) {
                content.deleteCharAt(content.length() - 1);
            }
            
            return content.toString();
        } catch (IOException e) {
            return "加载资源文件失败: " + e.getMessage();
        }
    }
    
    /**
     * 从当前类的资源路径加载文本内容
     * @param clazz 当前类的Class对象
     * @param fileName 文件名
     * @return 资源文件的内容，如果加载失败则返回错误信息
     */
    public static String loadResourceFileForClass(Class<?> clazz, String fileName) {
        // 构建资源路径：基于类的包路径
        String packageName = clazz.getPackage().getName().replace('.', '/');
        String resourcePath = "/" + packageName + "/" + fileName;
        
        return loadResourceFile(resourcePath);
    }
}