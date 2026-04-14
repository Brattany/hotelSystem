package com.manqiYang.hotelSystem.controller.common;

import com.manqiYang.hotelSystem.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @Value("${app.upload.base-dir:front_uploads}")
    private String uploadBaseDir;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type
    ) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String normalizedType = normalizeType(type);
        if (normalizedType == null) {
            return Result.error("上传类型仅支持 hotel 或 roomType");
        }

        try {
            Path uploadRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            Path targetDir = uploadRoot.resolve(normalizedType);
            Files.createDirectories(targetDir);

            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String extension = "";
            int lastDotIndex = originalName.lastIndexOf('.');
            if (lastDotIndex >= 0) {
                extension = originalName.substring(lastDotIndex);
            }

            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetFile = targetDir.resolve(uniqueFileName).normalize();
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/uploads/" + normalizedType + "/" + uniqueFileName;
            String accessUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(relativePath)
                    .toUriString();

            Map<String, String> result = new HashMap<>();
            result.put("path", relativePath);
            result.put("url", accessUrl);
            result.put("fileName", uniqueFileName);
            result.put("originalName", originalName);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }

        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("hotel".equals(normalized)) {
            return "hotel";
        }
        if ("roomtype".equals(normalized)) {
            return "roomType";
        }
        return null;
    }
}
