import RedirectButton from "./RedirectButton.jsx";
import VideoUploader from "./VideoUploader.jsx";
function HomePage(){
    return(
        <>
        <div className="w-screen h-screen flex-col">
        <div className="flex justify-center py-6">
        <h1 className="text-4xl font-extrabold mb-2 text-center animate-fade-in">
        Welcome to <span className="text-blue-400">StreamVerse</span>
      </h1>
      
        </div>

        {/* Redirect Button */}
        <RedirectButton />

        {/* Video Uploader */}
        <VideoUploader />
      </div>

        </>
    )
}
export default HomePage;