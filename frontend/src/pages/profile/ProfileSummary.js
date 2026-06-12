import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { addSpaces, getFirstAndLastWords, obfuscateEmail } from "../../utils";
import { fetchMyProfile, fetchMyUsers } from "../../features/auth/authSlice";

function ProfileSummary() {
  const { user } = useSelector((state) => state.auth);
  const dispatch = useDispatch();
  const { userProfile } = useSelector((state) => state.auth);

  async function fetchMyUsersData() {
    await dispatch(fetchMyProfile());
  }

  useEffect(() => {
    fetchMyUsersData();
  }, []);

  const colors = ["#FF5733", "#33FF57", "#3357FF", "#FF33A5", "#A533FF"];
  return (
    <>
      <div className="lg:px-[113px] px-3 mt-[40px] mb-[40px]">
        <div className="border border-[#E0E0E0] h-auto rounded-[12px] py-10 lg:gap-y-0 gap-y-4 lg:px-[60px] px-3 flex lg:flex-row flex-col lg:items-center items-start">
          <div className="name_summary">
            {getFirstAndLastWords(userProfile?.firstname)}
          </div>

          <div className="h-[73.03px] w-[1px] ml-[43px] border border-[#BDBDBD] lg:block hidden"></div>
          <div className="flex flex-col lg:ml-[60px] ml-0">
            <span className="typography_profile">{userProfile?.firstname}</span>
            <div className="mt-[8px] bg-[#388E3C] h-[35px] w-[86px] px-4 py-3 rounded-[4px] flex items-center justify-center text-white">
              {userProfile?.layer}
            </div>
          </div>

          <div className="lg:ml-[61px] ml-0 flex flex-col">
            <span className="label_2 !text-[#9E9E9E]">Role</span>
            <span className="label_1_profile ">{userProfile?.role}</span>
          </div>

          <div className="lg:ml-[80px] ml-0 flex flex-col">
            <span className="label_2 !text-[#9E9E9E]">Email address</span>
            <span className="label_1_profile ">
              {userProfile?.email ? obfuscateEmail(userProfile?.email) : null}
            </span>
          </div>

          <div className="lg:ml-[80px] ml-0 flex flex-col">
            <span className="label_2 !text-[#9E9E9E]">Phone number</span>
            <span className="label_1_profile ">
              +{addSpaces(userProfile?.phoneNumber)}
            </span>
          </div>

          <div className="lg:ml-[80px] ml-0 flex flex-col">
            <span className="label_2 !text-[#9E9E9E]">Permissions</span>
            <div className="flex flex-col">
              {userProfile?.permissions?.map((permission, index) => {
                const randomColor =
                  colors[Math.floor(Math.random() * colors.length)];
                return (
                  <div key={index} className="flex items-center gap-x-1 mb-1">
                    <span
                      className="w-[10px] h-[10px] rounded-full"
                      style={{ backgroundColor: randomColor }}
                    ></span>
                    <div>{permission}</div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default ProfileSummary;
