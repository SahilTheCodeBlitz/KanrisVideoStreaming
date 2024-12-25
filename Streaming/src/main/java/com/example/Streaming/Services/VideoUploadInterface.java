package com.example.Streaming.Services;

import com.example.Streaming.Model.Video;
import org.springframework.web.multipart.MultipartFile;

public interface VideoUploadInterface {
    // specifying the methods that we have to implement


    // method for saving the video that we get from front end to here temporary storage
    Video saveVideo(Video video, MultipartFile multipartFile);
}
