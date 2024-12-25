package com.example.Streaming.Services.implementation;
import com.example.Streaming.Model.Video;
import com.example.Streaming.Services.VideoUploadInterface;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@Service
public class VideoService implements VideoUploadInterface {

    private static final String Temp_Storage_Directory = "videos\\temp\\";

    private static final String TEMP_HSL = "videosHsl\\";


    @Override
    public Video saveVideo(Video video, MultipartFile multipartFile) {
        try {
            // Create the directory if it doesn't exist
            File tempDir = new File(Temp_Storage_Directory);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // Get the file extension from the original file name
            String originalFileName = multipartFile.getOriginalFilename();
            if (originalFileName == null) {
                throw new RuntimeException("Uploaded file does not have a name.");
            }

            // fetching the last extension of the file
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));

            // Using  videoId to generate the full file name
            String videoFileName = video.getVideoId() + fileExtension;

            // Path where the file will be stored
            Path path = Paths.get(Temp_Storage_Directory, videoFileName);

            // Copy the video file to the target directory
            Files.copy(multipartFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Populate the video object
            video.setContentType(multipartFile.getContentType());
            video.setFilePath(path.toString());


            // video saved here in temporary storage


            // Process the video using FFmpeg
            // second argument is the file path where the video is stored
            processVideo(video.getVideoId(),video.getFilePath());



            // Return the populated video object
            return video;

        } catch (Exception e) {
            throw new RuntimeException("Failure in saving the video", e);
        }
    }


    public void processVideo(String videoId, String videoPath) {

        System.out.println("input file path" +videoPath);
        // videoPath = path where video is already store

        try {

            // creating temporary output directory to store the video hsl file

            Path outputDir = Paths.get(TEMP_HSL, videoId);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);  // Ensure the output directory exists
            }

            System.out.println("Video Path = "+videoPath);// path where the video is stored
            System.out.println("output Dir = "+outputDir);// path where hsl master file and segment will be stored

            // Construct the FFmpeg command
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 3 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath,
                    outputDir,
                    outputDir
            );


            System.out.println(ffmpegCmd);
            //file this command

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr

            Process process = processBuilder.start();

            // just to see the logs of the ffmpeg command processing the video
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);  // Log output to console
            }





            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Video processing failed with exit code: " + exitCode);
            }

        } catch (IOException ex) {
            throw new RuntimeException("Video processing fail!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public ResponseEntity<?> fetchVideo(String videoId) {

        // get the path where the videoHsl playlist is stored
        // search whether we get directory with videoId there or not
        // if got then return the master file with appropriate headers

        try {
            Path path = Paths.get(TEMP_HSL, videoId, "master.m3u8");

            System.out.println(path);

            if (!Files.exists(path)) {
                // means files does not exist
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            FileSystemResource resource = new FileSystemResource(path);

            // if file is found return the master file with right headers

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl").header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store").body(resource);

        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the video.");
        }



    }

    public ResponseEntity<Resource> fetchSegment(String videoId, String segment) {

        Path path = Paths.get(TEMP_HSL,videoId,segment+".ts");
        System.out.println(path);

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE,"video/mp2t").body(resource);

    }
}
