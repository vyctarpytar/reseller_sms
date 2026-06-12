import React, { useState } from "react"; 
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { Radio } from "antd";
import InsideHeader from "../../../components/InsideHeader";
 
const plainOptions = ['Select individual people', 'Group'];
function GroupSelection() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [value1, setValue1] = useState('');
  const onChange1 = ({ target: { value } }) => {
 
    setValue1(value);
    if(value == "Group"){
      navigate("/group")
    }else{
      navigate('/group-individual-list')
    }
  };

  return (
    <div className="w-full overflow-y-scroll">
      <InsideHeader
        title="Group Selection"
        subtitle="Choose your group selection"
        back={true}
      />

      <div className="lg:px-10 px-3">
        <div className="flex flex-col mt-10">
          <div className="product_request_title !text-[31px]">
            SMS - Bulk Outbox
          </div>
          <div className="mt-5 paragraph">
            You have an option to choose which group you want to sms
          </div>
        </div>

        <div className="mt-[1rem]">
        <Radio.Group options={plainOptions} onChange={onChange1} value={value1} />
        </div>
      </div>
    </div>
  );
}

export default GroupSelection;
