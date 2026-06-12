import { Breadcrumb, Descriptions, message } from "antd";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchSandbox } from "../../features/sandbox/sandboxSlice";
import toast from "react-hot-toast";
import { dateForHumans } from "../../utils";

function SandBox() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { sandData } = useSelector((state) => state.sand);
 

  async function fetchSandboxData() {
    dispatch(fetchSandbox());
  }
  const handleCopyToClipboard = (text) => { 
    navigator?.clipboard
      ?.writeText(text)
      ?.then(() => {
        message.success("Copied to clipboard!");
      })
      .catch((err) => {
        message.error("Failed to copy!");
      });
  };

  const items = [
    {
      key: "1",
      label: "Status",
      children: sandData?.active ? "Active" : "Inactive",
    },

    {
      key: "3",
      label: "Api Key",
      children: (
        <div>
          {sandData?.apiKey ?? "N/A"}
          {sandData?.apiKey && (
            <button
              className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
              onClick={() => handleCopyToClipboard(sandData?.apiKey)}
            >
              Copy
            </button>
          )}
        </div>
      ),
    },

    {
      key: "5",
      label: "Client Name",
      children:(
        <div>{sandData?.clientName ?? "N/A"}</div>
      ) 
    },
    {
      key: "6",
      label: "Created Date",
      children:(
        <div>{dateForHumans(sandData?.createdDate) ?? "N/A"}</div>
      )  
    },
    {
      key: "7",
      label: "Expiry Date",
      children:(
        <div>{dateForHumans(sandData?.expirationDate) ?? "N/A"}</div>
      )   
    },
    {
      key: "8",
      label: "Endpoint",
      children: (
        <div>
          {sandData?.apiEndPoint ?? "N/A"}
          {sandData?.apiEndPoint && (
            <button
              className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
              onClick={() => handleCopyToClipboard(sandData?.apiEndPoint)}
            >
              Copy
            </button>
          )}
        </div>
      ),
    },
  ];

  const items2 = [
    {
      key: "1",
      label: "Single Curl",
      children: (
        <div>
          <pre style={{ whiteSpace: "pre-wrap" }}>
            {sandData?.apiPayload ?? "N/A"}
          </pre>
          {sandData?.apiPayload && (
            <button
              className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
              onClick={() => handleCopyToClipboard(sandData?.apiPayload)}
            >
              Copy
            </button>
          )}
        </div>
      ),
    },
    {
      key: "2",
      label: "Multiple Curl",
      children: (
        <div>
          <pre style={{ whiteSpace: "pre-wrap" }}>
            {sandData?.apiPayloadMultiple ?? "N/A"}
          </pre>
          {sandData?.apiPayloadMultiple && (
            <button
              className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
              onClick={() => handleCopyToClipboard(sandData?.apiPayloadMultiple)}
            >
              Copy
            </button>
          )}
        </div>
      ),
    },
  ];

  const formatJson = (jsonString) => {
    try {
      const jsonObject = JSON?.parse(jsonString);
      return JSON?.stringify(jsonObject, null, 2);
    } catch (error) {
      return jsonString;
    }
  };

  const items3 = [
    {
      key: "1",
      label: "Response",
      children: (
        <div className="mb-10">
          <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
            {sandData?.apiResponse ? formatJson(sandData?.apiResponse) : "N/A"}
          </pre>
          {sandData?.apiResponse && (
          <button
            className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
            onClick={() => handleCopyToClipboard(formatJson(sandData?.apiResponse))}
          >
            Copy
          </button>
        )}
        </div>
      ),
    },
  ];

  useEffect(() => {
    fetchSandboxData();
  }, []);

  return (
    <div className="overflow-y-scroll h-full w-full lg:px-10 px-3 mb-[30rem]">
      <div className="mt-[2.5rem]  font-dmSans text-[18px]">
        <Breadcrumb
          items={[
            {
              title: (
                <span
                  className="font-dmSans cursor-pointer "
                  onClick={() => navigate(-1)}
                >
                  Sandbox
                </span>
              ),
            },
            {
              title: (
                <span className="text-darkGreen font-dmSans">
                  Developer sandbox
                </span>
              ),
            },
          ]}
        />
      </div>
      <div className="product_request_title mt-[1.5rem]">SandBox</div>
      <div className="product_sub flex mt-[.5rem] !text-[18px]">
        Use the sandbox app to build and test your applications.
      </div>

      <div className="mt-[2.5rem] flex flex-shrink flex-wrap w-full gap-y-[1rem] ">
        <Descriptions
          column={1}
          colon={false}
          title="Api Information"
          items={items}
        />

        <Descriptions
          column={1}
          colon={false}
          title="Curl Information"
          items={items2}
        />

        <Descriptions
          column={1}
          colon={false}
          title="Response Information"
          items={items3}
        />
      </div>
    </div>
  );
}

export default SandBox;
