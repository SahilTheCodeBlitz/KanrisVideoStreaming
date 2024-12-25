import React, { useState } from "react";
import ReactPlayer from "react-player";

const VideoPlayer = () => {
  const [videoId, setVideoId] = useState("");
  const [hlsUrl, setHlsUrl] = useState("");
  const [error, setError] = useState(null);

  const handleLoadVideo = () => {
    if (videoId.trim()) {
      const apiUrl = `http://localhost:8080/video/api/fetch/${videoId}/master.m3u8`;
      setHlsUrl(apiUrl);
      setError(null); // Clear any previous errors
    } else {
      setError("Please enter a valid video ID.");
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-b from-gray-900 to-gray-700 text-white px-4">
      {/* Header */}
      <header className="text-center mt-10 sm:mt-16">
        <h1 className="text-4xl font-extrabold mb-4 animate-fade-in">
          Welcome to <span className="text-blue-400">StreamVerse</span>
        </h1>
        <p className="text-lg text-gray-300 italic">
          "Unleash the power of streaming. Your journey begins here!"
        </p>
      </header>

      {/* Video ID Input */}
      <section className="w-full max-w-lg mt-8 sm:mt-12 flex flex-col items-center">
        <input
          type="text"
          placeholder="Enter Video ID"
          className="w-full p-3 rounded-md border border-gray-500 text-gray-900 focus:outline-none focus:ring focus:border-blue-400"
          value={videoId}
          onChange={(e) => setVideoId(e.target.value)}
        />
        <button
          onClick={handleLoadVideo}
          className="mt-3 px-4 py-2 bg-blue-500 text-white font-semibold rounded-md hover:bg-blue-600 transition"
        >
          Load Video
        </button>
        {error && <p className="text-red-400 mt-2 text-sm">{error}</p>}
      </section>

      {/* Video Player */}
      <section className="relative w-full max-w-3xl bg-gray-800 rounded-xl shadow-lg mt-10 p-6">
        <div className="relative" style={{ paddingTop: "56.25%" }}>
          {error ? (
            <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-50 text-center text-white">
              <p className="text-red-400 text-lg mb-4">Error: {error}</p>
              <p className="text-gray-400">
                "Something went wrong. Refresh the page to try again."
              </p>
            </div>
          ) : hlsUrl ? (
            <ReactPlayer
              url={hlsUrl}
              playing={false}
              controls
              width="100%"
              height="100%"
              className="absolute top-0 left-0 rounded-lg overflow-hidden"
              config={{
                file: {
                  forceHLS: true,
                },
              }}
            />
          ) : (
            <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-50 text-center text-white">
              <p className="text-gray-300 text-xl mb-4"></p>
              <p className="text-gray-500 italic">
                "Enter a video ID to load your stream."
              </p>
            </div>
          )}
        </div>
      </section>

      {/* Footer */}
      <footer className="sm:mt-16 text-center mb-4">
        <p className="text-gray-400 italic text-sm sm:text-base">
          "Stream anywhere, anytime. Dive into the world of endless possibilities."
        </p>
      </footer>
    </div>
  );
};

export default VideoPlayer;
