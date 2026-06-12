import { notification } from "antd";
import moment from "moment";
import toast from "react-hot-toast";

{
  /* <Button
				onClick={() =>
					openNotificationWithIcon({
						key: '1',
						type: 'success',
						title: 'Title',
						content: 'Content',
					})
				}>
				Success
			</Button>
			<Button
				onClick={() =>
					openNotificationWithIcon({
						key: '2',
						type: 'info',
						title: 'Title',
						content: 'Content',
					})
				}>
				Info
			</Button>
			<Button
				onClick={() =>
					openNotificationWithIcon({
						key: '3',
						type: 'warning',
						title: 'Title',
						content: 'Content',
					})
				}>
				Warning
			</Button>
			<Button
				onClick={() =>
					openNotificationWithIcon({
						key: '4',
						type: 'error',
						title: 'Title',
						content: 'Content',
					})
				}>
				Error
			</Button> */
}

export const openNotificationWithIcon = (alertObj) => {
  notification[alertObj.type]({
    duration: 7,
    placement: "bottomLeft",
    stack: true,
    threshold: 1,
    key: alertObj.key,
    message: alertObj.title,
    description: alertObj.content,
  });
};

export const customToast = (toastObj) => {
  toast.custom((t) => (
    <div
      style={{
        border: `2px solid ${
          toastObj.bdColor === "error"
            ? "#C8001B"
            : toastObj.bdColor === "success"
            ? "#02A548"
            : "#285ff6"
        }`,
      }}
      className={`max-w-md w-full bg-white rounded-[3px] flex`}
    >
      <div className="flex-1 w-0 p-4">
        <div className="flex items-start">
          {toastObj?.img ? (
            <>
              <div className="flex-shrink-0 pt-0.5">
                <img
                  className="h-10 w-10 rounded-full"
                  src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-1.2.1&ixqx=6GHAjsWpt9&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2.2&w=160&h=160&q=80"
                  alt=""
                />
              </div>
            </>
          ) : null}

          <div className="ml-3 flex-1">
            {toastObj?.name ? (
              <>
                <p className="mt-1 text-[21px] text-blk3 font-bold">
                  {toastObj?.name}
                </p>
              </>
            ) : null}
            <p className="mt-1 text-[18px] text-blk3 font-semibold">
              {toastObj?.content}
            </p>
          </div>
        </div>
      </div>
      <div
        style={{
          borderLeft: `2px solid ${
            toastObj.bdColor === "error"
              ? "#C8001B"
              : toastObj.bdColor === "success"
              ? "#02A548"
              : "#285ff6"
          }`,
        }}
        className="flex"
      >
        <button
          onClick={() => toast.dismiss(t.id)}
          className="w-full p-4 flex items-center justify-center"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="27"
            height="27"
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M13.408 11.9963L17.708 7.70628C17.8963 7.51798 18.0021 7.26258 18.0021 6.99628C18.0021 6.72998 17.8963 6.47458 17.708 6.28628C17.5197 6.09798 17.2643 5.99219 16.998 5.99219C16.7317 5.99219 16.4763 6.09798 16.288 6.28628L11.998 10.5863L7.70799 6.28628C7.51968 6.09798 7.26429 5.99219 6.99799 5.99219C6.73168 5.99219 6.47629 6.09798 6.28799 6.28628C6.09968 6.47458 5.99389 6.72998 5.99389 6.99628C5.99389 7.26258 6.09968 7.51798 6.28799 7.70628L10.588 11.9963L6.28799 16.2863C6.19426 16.3792 6.11986 16.4898 6.06909 16.6117C6.01833 16.7336 5.99219 16.8643 5.99219 16.9963C5.99219 17.1283 6.01833 17.259 6.06909 17.3809C6.11986 17.5027 6.19426 17.6133 6.28799 17.7063C6.38095 17.8 6.49155 17.8744 6.61341 17.9252C6.73527 17.9759 6.86597 18.0021 6.99799 18.0021C7.13 18.0021 7.2607 17.9759 7.38256 17.9252C7.50442 17.8744 7.61502 17.8 7.70799 17.7063L11.998 13.4063L16.288 17.7063C16.3809 17.8 16.4915 17.8744 16.6134 17.9252C16.7353 17.9759 16.866 18.0021 16.998 18.0021C17.13 18.0021 17.2607 17.9759 17.3826 17.9252C17.5044 17.8744 17.615 17.8 17.708 17.7063C17.8017 17.6133 17.8761 17.5027 17.9269 17.3809C17.9776 17.259 18.0038 17.1283 18.0038 16.9963C18.0038 16.8643 17.9776 16.7336 17.9269 16.6117C17.8761 16.4898 17.8017 16.3792 17.708 16.2863L13.408 11.9963Z"
              fill={`${
                toastObj.bdColor === "error"
                  ? "#C8001B"
                  : toastObj.bdColor === "success"
                  ? "#02A548"
                  : "#285ff6"
              }`}
            />
          </svg>
        </button>
      </div>
    </div>
  ));
};
export const numberWithCommas = (money) => {
  return Number(money)
    .toLocaleString({
      style: "currency",
      currency: "KES",
    })
    .toLowerCase();
};

export function capitalize(s) {
  return s && s[0].toUpperCase() + s.slice(1);
}

export function cashConverter(money) {
  return capitalize(
    Number(money).toLocaleString("en-US", {
      style: "currency",
      currency: "KES",
    })
  );
}


export const formatMoney = (money) => {
  let keCurrency = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "KES",
  });
  if (!money) {
    return "";
  }
  return keCurrency.format(money);
};

export const obfuscateEmail = (email) => {
  const [username, domain] = email?.split("@");

  if (!username || !domain) {
    throw new Error("Invalid email format");
  }
  const obfuscatedUsername = username?.substring(0, 2) + "****";
  return `${obfuscatedUsername}@${domain}`;
};

export const addSpaces = (number) => {
  const numberStr = number?.toString();
  const formattedStr = numberStr?.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return formattedStr;
};

export const getFirstAndLastWords = (text) => {
  var text_arr = text?.split("");
  if (text_arr?.length) {
    return String(text_arr[0] + text_arr[text_arr.length - 1]).toUpperCase();
  }
  return "";
};

export const previewPaper = (doc) => {
  let link = formatImgPath(doc);
  if (
    getFileType(doc) == "docx" ||
    getFileType(doc) == "doc" ||
    getFileType(doc) == "pdf"
  ) {
    window.open(`${link}`, "_blank");
  } else {
    window.open(link, "_blank");
  }
};

const getFileType = (file) => {
  return file?.substring(file.lastIndexOf(".") + 1);
};
export const formatPath = (path) => {
  if (!path) return "";
  return path?.substring(path.lastIndexOf("/") + 1);
};
export const formatImgPath = (path) => {
  if (
    path === null ||
    path === "" ||
    path === undefined ||
    typeof path === "object"
  ) {
    return path;
  }

  if (!path.startsWith("./")) {
    return path;
  }
  const host = window.location.host;
  const protocol = window.location.protocol;
  const domain = "sms.smartgate.co.ke";

  
  if (host === "localhost:3000") {
    return `${protocol}//${domain}${path.replace("./", "/")}`;
  } else if (host === "localhost:5173") {
    return `${protocol}//${domain}${path.replace("./", "/")}`;
  } else {
    return `https://${domain}${path.replace("./", "/")}`;
  }
};

export const dateForHumans = (timestamp) => {
  if (!timestamp) return;
  let date = new Date(timestamp);

  let day = date.getDate();
  let month = new Intl.DateTimeFormat("en-US", { month: "long" }).format(date);
  let year = date.getFullYear();

  let dayString = day + getDaySuffix(day);

  return `${dayString} ${month} ${year}`;
};

function getDaySuffix(day) {
  if (day >= 11 && day <= 13) {
    return "th";
  }
  switch (day % 10) {
    case 1:
      return "st";
    case 2:
      return "nd";
    case 3:
      return "rd";
    default:
      return "th";
  }
}

export const removeCommas = (numberString) => {
 
  if (typeof numberString !== 'string' || numberString === null || numberString === undefined) {
 
    return numberString;
  }

  return numberString.replace(/,/g, '');
}
export const removeNegative=(number)=> {
  return Math.abs(number);
}

export const disabledDate = current => { 
  return current && current > moment().startOf('day');
};
 
export const disabledPastDate = (current) => {
  return current && current < moment().startOf("day");
};


export const getFirstLetter = (word) => {
  if (!word || typeof word !== "string") return "Invalid input";

  return word.charAt(0);
};

export const getLetterWord = (sentence) => {
  if (!sentence || typeof sentence !== "string") return "Invalid input";
  const words = sentence.split(" ");
  const firstLetters = words.map((word) => word.charAt(0));

  return firstLetters.join("");
};
 
// export const getSubdomain = () => {
//   const hostname = window?.location?.hostname;
   
//   if (!hostname) return null;
 
//   const parts = hostname?.split('.'); 
 
//   if (parts?.length >= 3) {
//     return parts[parts?.length - 3];  
//   }
 
//   return "localhost";
// };

export const getSubdomain = () => {
  const hostname = window?.location?.hostname;
   
  if (!hostname) return null;

  const parts = hostname.split('.'); 
  const commonTLDs = ['com', 'co', 'org', 'net'];

  if (parts.length >= 2) {
    const tld = parts[parts.length - 1];   

    if (commonTLDs.includes(tld)) { 
      return parts[parts.length - 2];
    } else if (parts.length >= 3 && commonTLDs.includes(parts[parts.length - 2])) { 
      return parts[parts.length - 3];
    }
  }

  return "localhost";  
};


export const formatDate = (dateString) => {
  const date = new Date(dateString);
  const year = date?.getFullYear();
  const month = String(date?.getMonth() + 1)?.padStart(2, '0');
  const day = String(date?.getDate())?.padStart(2, '0');
  return `${year}-${month}-${day}`;
};
export const getDate7DaysAgo=()=>{
  const today = new Date();
  const sevenDaysAgo = new Date(today);
  sevenDaysAgo.setDate(today?.getDate() - 7); 
  const year = sevenDaysAgo?.getFullYear();
  const month = String(sevenDaysAgo?.getMonth() + 1)?.padStart(2, "0");
  const day = String(sevenDaysAgo?.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
export const getDate30DaysAgo=()=>{
  const today = new Date();
  const thirtyDaysAgo = new Date(today);
  thirtyDaysAgo.setDate(today?.getDate() - 30); 
  const year = thirtyDaysAgo?.getFullYear();
  const month = String(thirtyDaysAgo?.getMonth() + 1)?.padStart(2, "0");
  const day = String(thirtyDaysAgo?.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export const getDate90DaysAgo=()=>{
  const today = new Date();
  const nityDaysAgo = new Date(today);
  nityDaysAgo.setDate(today?.getDate() - 90); 
  const year = nityDaysAgo?.getFullYear();
  const month = String(nityDaysAgo?.getMonth() + 1)?.padStart(2, "0");
  const day = String(nityDaysAgo?.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
 
export const getTomorrowDateMorning=()=>{
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow?.setDate(tomorrow.getDate() + 1);
 
  const year = tomorrow?.getFullYear();
  const month = String(tomorrow?.getMonth() + 1)?.padStart(2, "0");
  const day = String(tomorrow?.getDate())?.padStart(2, "0");

  return `${year}-${month}-${day} 08:00`; 
}

export const getTomorrowDateAfternoon=()=>{
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow?.setDate(tomorrow.getDate() + 1);
 
  const year = tomorrow?.getFullYear();
  const month = String(tomorrow?.getMonth() + 1)?.padStart(2, "0");
  const day = String(tomorrow?.getDate())?.padStart(2, "0");

  return `${year}-${month}-${day} 1:00`; 
}

export const formatDateTime=(dateTimeStr)=> {
  const date = new Date(dateTimeStr);

  const datePart = date?.toISOString()?.split('T')[0];
  let hours = date?.getHours();
  const minutes = date?.getMinutes()?.toString()?.padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
 
  hours = hours % 12;
  hours = hours ? hours : 12;  

  const timePart = `${hours}:${minutes} ${ampm}`;
 
  return `${datePart} ${timePart}`;
}
export const normalizeDateToLocalYearWTime = (date) => {
  if (!date) return null;  
  const jsDate = new Date(date); 
  const year = jsDate?.getFullYear();
  const month = String(jsDate?.getMonth() + 1)?.padStart(2, '0'); 
  const day = String(jsDate?.getDate())?.padStart(2, '0');
  return `${year}-${month}-${day}T00:00:00`;
};
 

export const normalizeDateToLocalYear = (date) => {
  if (!date) return null;
  const jsDate = new Date(date);
  const year = jsDate?.getFullYear();
  const month = String(jsDate?.getMonth() + 1)?.padStart(2, "0");
  const day = String(jsDate?.getDate())?.padStart(2, "0");
  return `${year}-${month}-${day}`;
};


export const normalizeDateToLocalYearOnly = (date) => {
  if (!date) return null;
  const jsDate = new Date(date);
  const year = jsDate?.getFullYear();
  const month = String(jsDate?.getMonth() + 1)?.padStart(2, "0");
  const day = String(jsDate?.getDate())?.padStart(2, "0");
  return `${year}`;
};


export const formatText = (str) => {
  if (typeof str !== "string") return "";
  return str
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(" ");
};


 