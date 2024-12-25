import { useState } from "react";
import axios from "axios";
import UploadImg from "../assets/upload.png";


function VideoUploader(){
    const [meta, setMeta] = useState({ title: "", description: "" });
    const [progress, setProgress] = useState(0); // Track upload progress
    const [uploading, setUploading] = useState(false); // Track upload state
    const [uploadStatus, setUploadStatus] = useState(""); // Upload status message
  
    const handleSubmit = async (event) => {
      event.preventDefault();
  
      const formData = new FormData(event.target);
  
      const file = formData.get("file");
      const videoTitle = formData.get("title");
      const videoDescription = formData.get("description");
  
      if (!file) {
        setUploadStatus("Please upload a file.");
        return;
      }
  
      const requestData = new FormData();
      requestData.append("file", file);
      requestData.append("title", videoTitle);
      requestData.append("description", videoDescription);
  
      try {
        setUploading(true);
        setUploadStatus("");
        const response = await axios.post("http://localhost:8080/video/api/upload", requestData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
          onUploadProgress: (progressEvent) => {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setProgress(percentCompleted);
          },
        });
  
        setUploadStatus("Uploaded successfully!");
        console.log("Upload Successful:", response.data);
      } catch (error) {
        setUploadStatus("Error during upload. Please try again.");
        console.error("Upload Failed:", error.response || error.message);
      } finally {
        setUploading(false);
        setProgress(0); // Reset progress after completion
      }
    };
  
    return (
        <>
        <div className="flex flex-col items-center py-4 ">
        <form
          onSubmit={handleSubmit}
          className="bg-[#374151] flex flex-col items-center space-y-6 border w-fit h-fit border-gray-50 p-12 rounded-2xl shadow-lg"
        >
          <div className="shrink-0">
            <img
              className="h-16 w-16 object-cover"
              src={UploadImg}
              alt="Current profile photo"
            />
          </div>

          <div className="w-full">
            <label className="block mb-2 text-sm font-bold text-white">
              Video Title
            </label>
            <input
              type="text"
              name="title"
              placeholder="Enter video title"
              className="block w-full px-3 py-2 text-sm border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-violet-500 text-black"
            />
          </div>

          <div className="w-full">
            <label className="block mb-2 text-sm  text-white font-bold">
              Video Description
            </label>
            <textarea
              placeholder="Enter video description"
              name="description"
              rows="3"
              className="text-black block w-full px-3 py-2 text-sm border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-violet-500"
            />
          </div>

          <label className="block">
            <span className="sr-only">Choose a file</span>
            <input
              type="file"
              name="file"
              className="block w-full text-sm text-slate-500
                file:mr-4 file:py-2 file:px-4
                file:rounded-full file:border-0
                file:text-sm file:font-semibold
                file:bg-violet-50 file:text-violet-700
                hover:file:bg-violet-100
              "
            />
          </label>

          <button
            type="submit"
            className="px-6 py-2 bg-blue-400 text-white font-semibold rounded-lg shadow-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-opacity-75"
            disabled={uploading}
          >
            {uploading ? "Uploading..." : "Submit"}
          </button>
        </form>

        {/* Progress Bar */}
        {uploading && (
          <div className="w-full mt-4">
            <div className="relative pt-1">
              <div className="flex mb-2 items-center justify-between">
                <div>
                  <span className="text-xs font-semibold inline-block py-1 px-2 uppercase rounded-full text-blue-600 bg-blue-200">
                    Upload Progress
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-xs font-semibold inline-block text-blue-600">
                    {progress}%
                  </span>
                </div>
              </div>
              <div className="overflow-hidden h-2 mb-4 text-xs flex rounded bg-blue-200">
                <div
                  style={{ width: `${progress}%` }}
                  className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-blue-500"
                ></div>
              </div>
            </div>
          </div>
        )}

        {/* Upload Status Message */}
        {uploadStatus && (
          <div className={`mt-4 text-sm font-medium ${uploadStatus.includes("Error") ? "text-red-500" : "text-green-500"}`}>
            {uploadStatus}
          </div>
        )}
      </div>
        </>
    )
}
export default VideoUploader;