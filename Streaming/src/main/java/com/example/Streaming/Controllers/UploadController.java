package com.example.Streaming.Controllers;

import com.example.Streaming.Model.Video;
import com.example.Streaming.Services.implementation.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("video/api")
public class UploadController {

    @Autowired
    VideoService videoService;

    @GetMapping
    @RequestMapping("/test")
    public String testFunc(){
        return "Success is success";
    }

    @PostMapping
    @RequestMapping("/upload")
    public String uploadVideo(@RequestParam("file")MultipartFile multipartFile , @RequestParam("title")String title,
                              @RequestParam("description") String description ){

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        try{
            Video vid = (Video) videoService.saveVideo(video,multipartFile);

        }catch(Exception e){
            throw  new RuntimeException("Failure",e);
        }



        return "Success";
    }



    // creating the controller to send the master file to the frontend so he can stream


    @GetMapping
    @RequestMapping("/fetch/{videoId}/master.m3u8")
    public ResponseEntity<?> fetch(@PathVariable("videoId") String videoId ){

        return videoService.fetchVideo(videoId);

    }

    // for serving the segments
    @GetMapping
    @RequestMapping("/fetch/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> serveSegments(@PathVariable String videoId , @PathVariable String segment){

        return videoService.fetchSegment(videoId,segment);

    }

}
