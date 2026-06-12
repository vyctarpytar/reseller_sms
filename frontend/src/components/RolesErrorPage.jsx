import React from "react";
import "./error.css";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";

function RolesErrorPage() {
  const navigate = useNavigate();
  const { user } = useSelector((state) => state.auth);
  const handleDash = () => {
    if (user?.layer === "ACCOUNT") {
      navigate("/dashboard-account");
    } else {
      navigate("/dashboard");
    }
  };
  return (
    <div class="container bg-[#141829] h-[100vh] flex items-center justify-center">
      <div class="row"> 
        <div class="col-lg-12">
          <div class="col-lg-12 text-404">
            <b>404</b>
          </div>
          <div class="col-lg-12 text">
            <b>YOU DON'T HAVE PERMISSION TO ACCESS THIS PAGE</b>
          </div>
          <div class="col-lg-12 text-btn">
            <button
              name="login"
              class="btn btn-outline-primary"
              onClick={handleDash}
            >
              BACK
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default RolesErrorPage;
