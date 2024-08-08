package com.hmdp.controller;

import com.hmdp.MinIO.FileService;
import com.hmdp.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/test")
    public Result test() {
        return Result.ok("test");
    }


    @PostMapping("/image/upload")
    public Result uploadImage(MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }

    @CrossOrigin
    @PostMapping("/file/upload")
    public Result uploadFile(MultipartFile file) {
        return Result.ok(fileService.uploadFile(file));
    }

}