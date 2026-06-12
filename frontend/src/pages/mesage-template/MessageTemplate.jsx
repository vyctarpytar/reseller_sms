import React, { useState, useRef, useEffect } from "react";
import "./mention.css";
import { Form, Input } from "antd";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { fetchPlaceholders } from "../../features/sms-request/smsRequestSlice";

const { TextArea } = Input;
const MessageTemplate = ({ inputValue, setInputValue }) => {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [mentions, setMentions] = useState([]);
  const [dropdownVisible, setDropdownVisible] = useState(false);
  const [dropdownItems] = useState(["firstname", "accMsgBal"]);

  const {customPlaceholder} = useSelector((state)=>state.sms) 
  const inputRef = useRef(null);

  const handleInputChange = (e) => {
    const value = e.target.value;
    setInputValue(value);

    const lastWord = value.split(" ").pop();
    if (lastWord.startsWith("@")) {
      setDropdownVisible(true);
    } else {
      setDropdownVisible(false);
    }
  };

  const handleDropdownClick = (mention) => {
    const words = inputValue.split(" ");
    words.pop();
    const newValue = [...words, `@${mention}`].join(" ");
    setInputValue(newValue);
    setDropdownVisible(false);
    inputRef.current.focus();
  };

  function fetchPlaceholderData(){
    dispatch(fetchPlaceholders())
  };

  useEffect(()=>{
    fetchPlaceholderData()
  },[])

  return (
    <div className="mention-container">
      <span>
        Custom Message <span className="text-[#FF0000]">*</span>
      </span>

      <TextArea
        ref={inputRef}
        value={inputValue}
        onChange={handleInputChange}
        placeholder="Type your message here..."
        rows={4}
      />
      {dropdownVisible && (
        <div className="dropdown">
          {customPlaceholder.map((item) => (
            <div
              key={item}
              className="dropdown-item"
              onClick={() => handleDropdownClick(item)}
            >
              {item}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MessageTemplate;
