import React from "react";
import { useNavigate } from "react-router-dom";



const RedirectButton = () => {

  const navigate = useNavigate();

  const handleClick = (event) => {
    event.preventDefault();
    navigate("/stream")  
  };

  return (
    <div className="flex justify-center">

        <button
          onClick={handleClick}
          className="px-4 py-2 bg-blue-500 text-white font-semibold rounded-md hover:bg-blue-600 transition"
        >
          Stream Video
        </button>

    </div>
    
  );
};

export default RedirectButton;
