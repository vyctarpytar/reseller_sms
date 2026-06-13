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

  // ── Account & top-up endpoints (balance / load / invoice-status) ──────────────
  // Docs are built client-side from the issued key so they stay in sync with it. The base is
  // derived from the existing single-sms endpoint so it tracks the same host.
  const apiBase = (
    sandData?.apiEndPoint || "https://backend.synqafrica.co.ke/api/v2/sandbox/single-sms"
  ).replace(/\/single-sms$/, "");
  const apiKeyVal = sandData?.apiKey || "YOUR_API_KEY";

  const codeBlock = (text) => (
    <div>
      <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>{text}</pre>
      <button
        className="ml-[10px] cursor-pointer text-[#F18114] font-[600] text-[1.2rem]"
        onClick={() => handleCopyToClipboard(text)}
      >
        Copy
      </button>
    </div>
  );

  const balanceCurl = `curl --request GET \\
  --url ${apiBase}/balance \\
  --header 'X-API-KEY: ${apiKeyVal}'`;

  const loadCurl = `curl --request POST \\
  --url ${apiBase}/load \\
  --header 'Content-Type: application/json' \\
  --header 'X-API-KEY: ${apiKeyVal}' \\
  --data '{
  "smsPayAmount": 1000,
  "smsPayerMobileNumber": "254712345678",
  "smsLoadingMethod": "MONEY"
}'`;

  const statusCurl = `curl --request GET \\
  --url ${apiBase}/invoice-status/SMS0001ABCD \\
  --header 'X-API-KEY: ${apiKeyVal}'`;

  const balanceResp = `{
  "success": true,
  "data": { "result": {
    "accName": "Your Company",
    "accStatus": "ACTIVE",
    "accBalance": 142.00,
    "accRate": 0.50,
    "accUnits": 284
  } },
  "status": 200
}`;

  const loadResp = `{
  "success": true,
  "messages": { "message": "STK pop for amount 1000 to code SMS0001ABCD" },
  "data": { "result": {
    "invoCode": "SMS0001ABCD",
    "invoStatus": "PENDING_PAYMENT",
    "invoAmount": 1000
  } },
  "status": 200
}`;

  const statusResp = `{
  "success": true,
  "data": { "result": {
    "invoiceCode": "SMS0001ABCD",
    "status": "PAID",
    "paid": true,
    "amount": 1000,
    "failureReason": null,
    "mpesaReceipt": "TXXXXXXXXX"
  } },
  "status": 200
}`;

  const items4 = [
    {
      key: "1",
      label: "Get SMS Balance — GET /balance",
      children: (
        <div>
          <div className="mb-2">
            Returns the account SMS balance (KES) and the equivalent number of units.
          </div>
          {codeBlock(balanceCurl)}
          <div className="mt-3 mb-1 font-[600]">Sample response</div>
          {codeBlock(balanceResp)}
        </div>
      ),
    },
    {
      key: "2",
      label: "Load SMS in KES — POST /load",
      children: (
        <div>
          <div className="mb-2">
            Creates an invoice and sends an M-Pesa STK push to{" "}
            <span className="font-[600]">smsPayerMobileNumber</span>. The balance is credited once
            the payment settles — poll the returned{" "}
            <span className="font-[600]">invoCode</span> with the invoice-status endpoint to know when
            it&apos;s done.
          </div>
          {codeBlock(loadCurl)}
          <div className="mt-3 mb-1 font-[600]">Sample response</div>
          {codeBlock(loadResp)}
        </div>
      ),
    },
    {
      key: "3",
      label: "Invoice Status — GET /invoice-status/{invoiceCode}",
      children: (
        <div>
          <div className="mb-2">
            Polls a load invoice. <span className="font-[600]">paid: true</span> (status{" "}
            <span className="font-[600]">PAID</span>) means the top-up is complete. Terminal failure
            statuses are <span className="font-[600]">FAILED</span>,{" "}
            <span className="font-[600]">CANCELLED</span> and{" "}
            <span className="font-[600]">EXPIRED</span>.
          </div>
          {codeBlock(statusCurl)}
          <div className="mt-3 mb-1 font-[600]">Sample response</div>
          {codeBlock(statusResp)}
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
                <span className="text-primary font-dmSans">
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

        <Descriptions
          column={1}
          colon={false}
          title="Account & Top-up Endpoints"
          items={items4}
        />
      </div>
    </div>
  );
}

export default SandBox;
