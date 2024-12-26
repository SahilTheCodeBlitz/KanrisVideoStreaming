package com.example.Streaming.Services.implementation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.example.Streaming.Model.Video;
import com.example.Streaming.Services.VideoUploadInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
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
import java.util.Objects;


@Service
public class VideoService implements VideoUploadInterface {

    private static final String Temp_Storage_Directory = "videos\\temp\\";

    private static final String TEMP_HSL = "videosHsl\\";

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;


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



            // deleting the temporary files

            // Clean up temporary files
            cleanupTemporaryFiles(video.getVideoId(), video.getFilePath().toString());



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


            // uploading the videoHsl folder to the aws s3

            // Upload processed files to S3
            File outputDirectory = outputDir.toFile();
            uploadDirectoryToS3(outputDirectory, "hls/" + videoId);




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


    public void uploadDirectoryToS3(File directory, String s3Path) {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("The provided directory is invalid.");
        }

        try {
            // Iterate through files in the directory
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isFile()) {
                    // Define the S3 key (path within the bucket)
                    String key = s3Path + "/" + file.getName();

                    System.out.println("Uploading: " + file.getAbsolutePath() + " to S3 key: " + key);

                    // Upload the file
                    amazonS3.putObject(bucketName, key, new FileInputStream(file), new ObjectMetadata());
                } else if (file.isDirectory()) {
                    // Handle subdirectories recursively
                    uploadDirectoryToS3(file, s3Path + "/" + file.getName());
                }
            }
            System.out.println("Directory uploaded successfully to S3.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload directory to S3", e);
        }
    }



    // two function for sending master file and the segment file from s3 bucket to frontend


    // method for sending the master file
    public ResponseEntity<?> fetchVideoS3(String videoId) {
        try {
            // S3 key for the master.m3u8 file
            String key = "hls/" + videoId + "/master.m3u8";


            // Check if the file exists in S3
            if (!amazonS3.doesObjectExist(bucketName, key)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Get the file as a S3Object
            S3Object s3Object = amazonS3.getObject(bucketName, key);

            // Stream the file content
            InputStream inputStream = s3Object.getObjectContent();
            byte[] content = inputStream.readAllBytes();

            // Return the content with appropriate headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                    .body(content);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the video: " + e.getMessage());
        }
    }


    // function for sending the segments from s3 to the client

    public ResponseEntity<Resource> fetchSegmentS3(String videoId, String segment) {
        try {
            // S3 key for the specific segment file
            String key = "hls/" + videoId + "/" + segment + ".ts";


            // Check if the file exists in S3
            if (!amazonS3.doesObjectExist(bucketName, key)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Get the file as a S3Object
            S3Object s3Object = amazonS3.getObject(bucketName, key);

            // Stream the file content as a resource
            InputStream inputStream = s3Object.getObjectContent();
            Resource resource = new InputStreamResource(inputStream);

            // Return the resource with appropriate headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp2t")
                    .body(resource);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // function for deleting the temporary data from storage

    private void cleanupTemporaryFiles(String videoId, String videoFilePath) {
        try {
            // Delete the video file
            File videoFile = new File(videoFilePath);
            if (videoFile.exists() && videoFile.isFile()) {
                videoFile.delete();
                System.out.println("Deleted video file: " + videoFilePath);
            }

            // Delete the HLS output directory
            Path hlsOutputDir = Paths.get(TEMP_HSL, videoId);
            if (Files.exists(hlsOutputDir)) {
                Files.walk(hlsOutputDir)
                        .map(Path::toFile)
                        .sorted((a, b) -> b.compareTo(a)) // Ensure files are deleted before directories
                        .forEach(File::delete);
                System.out.println("Deleted HLS directory: " + hlsOutputDir);
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }




}
