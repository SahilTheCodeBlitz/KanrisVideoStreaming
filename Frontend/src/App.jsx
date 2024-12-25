import VideoPlayer from "./component/VideoPlayer.jsx"
import Hls from 'hls.js'
import videojs from 'video.js'
import { useEffect } from "react"
import VideoUploader from "./component/VideoUploader.jsx"
import './componentcss/App.css';

import HomePage from "./component/HomePage.jsx"
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Link,
  useNavigate,
  Outlet,
} from "react-router-dom";



function App() {



  return (
    <>
  <Router>
      
      {/* Define Routes */}
      <Routes>
        <Route path="/" element={<HomePage/>} />
        <Route path="/stream" element={<VideoPlayer />} />
      </Routes>
    </Router>
     
    </>
  )
}

export default App
