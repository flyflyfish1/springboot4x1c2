package com.controller;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.ConfigEntity;
import com.entity.EIException;
import com.service.ConfigService;
import com.utils.HadoopTemplate;
import com.utils.R;

@RestController
@RequestMapping("file")
@SuppressWarnings({"unchecked","rawtypes"})
public class FileController {
    @Autowired
    private ConfigService configService;

    @Autowired
    private HadoopTemplate hadoopTemplate;

    @RequestMapping("/upload")
    @IgnoreAuth
    public R upload(@RequestParam("file") MultipartFile file, String type) throws Exception {
        if (file.isEmpty()) {
            throw new EIException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename) || originalFilename.lastIndexOf(".") == -1) {
            throw new EIException("上传文件格式不正确");
        }

        String fileExt = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        File uploadDir = new File(System.getProperty("user.dir"), "upload");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new EIException("上传目录创建失败");
        }

        String fileName = System.currentTimeMillis() + "." + fileExt;
        if (StringUtils.isNotBlank(type) && type.contains("_template")) {
            fileName = type + "." + fileExt;
            new File(uploadDir, fileName).deleteOnExit();
        }

        File dest = new File(uploadDir, fileName);
        file.transferTo(dest);
        try {
            // HDFS 同步失败时不影响本地上传结果
            hadoopTemplate.uploadFile(dest.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (StringUtils.isNotBlank(type) && type.equals("1")) {
            ConfigEntity configEntity = configService.selectOne(new EntityWrapper<ConfigEntity>().eq("name", "faceFile"));
            if (configEntity == null) {
                configEntity = new ConfigEntity();
                configEntity.setName("faceFile");
            }
            configEntity.setValue(fileName);
            configService.insertOrUpdate(configEntity);
        }
        return R.ok().put("file", fileName);
    }

    @IgnoreAuth
    @RequestMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String fileName) {
        byte[] ioBuffer = hadoopTemplate.download(fileName);
        if (ioBuffer.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            return new ResponseEntity<byte[]>(ioBuffer, headers, HttpStatus.CREATED);
        }
        return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
