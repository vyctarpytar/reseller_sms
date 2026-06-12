import { Dropdown, Table } from "antd";
import moment from "moment";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  fetchContactFolders,
  fetchContacts,
  saveGraduateHeader,
  setGradUpload,
  setGraduateListObj,
} from "../../../features/contact/ContactSlice";
import { useNavigate } from "react-router-dom"; 
import InsideHeader from "../../../components/InsideHeader";
import container from "../../../assets/img/Container.png";  
import AddContactListModal from "./modal/AddContactListModal";
import HowToAddContactModal from "./modal/HowToAddContactModal"; 
import ContactsListView from "./ContactsListView";

export default function ContactFolderView() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { folderObj } = useSelector((state) => state.folder);
  const { gradLoading, contactsHeaders, contacts } = useSelector(
    (state) => state.contact
  );

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [hasSelected, sethasSelected] = useState(false);
  const [open, setopen] = useState(false);
  const [openGraduate, setopenGraduate] = useState(false);
  const [activeGradListObj, setactiveGradListObj] = useState({});

  function handleCancel() {
    setopen(false);
  }

  function handleCancelGraduate() {
    setopenGraduate(false);
  }

  function handleAddNew() {
    setopen(true);
    setopenGraduate(false);
  }

  const handleGraduateOption = () => {
    setopenGraduate(true);
  };

  async function handleUploadFile() {
    await dispatch(setGradUpload(false));
    await navigate("/contacts/add/file-upload");
  }

  async function handleGradListObj(item) {
    await dispatch(setGraduateListObj(item));
    await setactiveGradListObj(item);
    // await navigate(`/contacts/folders/list/${item?.gdcTitle}`)
  }

  async function handleSubmitToNea() {
    await selectedRowKeys?.forEach((item) => {
      let updObj = {
        gdcId: item,
        gdcStatus: "SUBMITTED",
      };
      dispatch(saveGraduateHeader(updObj));
    });
    await handleFetchData();
    await setSelectedRowKeys([]);
    await sethasSelected(false);
  }

 
  async function handleFetchData() { 
    await dispatch(fetchContactFolders(folderObj?.groupId));
  }

  
  const handleGoBack = ()=>{
    navigate('/group')
  }


  const settingItems = [
    {
      key: "0",
      label: (
        <button
          onClick={() =>
            navigate(`/contacts/folders/list/${activeGradListObj?.gdcTitle}`)
          }
          className="font-dmSans text-black font-[400] text-[19px] !mt-[5%]"
        >
          View list
        </button>
      ),
    },

    {
      key: "1",
      label: (
        <div
          onClick={handleUploadFile}
          className="font-dmSans text-black font-[400] text-[19px] !mt-[5%]"
        >
          Upload .csv
        </div>
      ),
    },

    {
      key: "2",
      label: (
        <div
          onClick={
            activeGradListObj?.gdcStatus === "SUBMITTED"
              ? null
              : handleSubmitToNea
          }
          className="font-dmSans text-black font-[400] text-[19px] !mt-[5%]"
        >
          {activeGradListObj?.gdcStatus === "SUBMITTED"
            ? "Submitted"
            : "Submit to NEA"}
        </div>
      ),
    },
  ];

  const columns = [
    {
      title: "Name of List",
      dataIndex: "gdcTitle",
    },
    {
      title: "Updated By",
      dataIndex: "gdcUpdatedBy",
    },
    {
      title: "Date Created",
      dataIndex: "gdcCreatedDate",

      render: (item) => <span>{moment(item).format("Do MMMM YYYY")}</span>,
    },
    {
      title: "Status",
      dataIndex: "gdcStatus",
    },
    {
      title: "",
      render: (item) => (
        <>
          {/* <button onClick={() =>handleGradListObj(item)}>
						<svg
							xmlns='http://www.w3.org/2000/svg'
							width='27'
							height='24'
							viewBox='0 0 27 24'
							fill='none'>
							<path
								d='M23.9436 11.6C21.8099 6.91 17.7959 4 13.4651 4C9.13428 4 5.12035 6.91 2.98662 11.6C2.92846 11.7262 2.89844 11.8623 2.89844 12C2.89844 12.1377 2.92846 12.2738 2.98662 12.4C5.12035 17.09 9.13428 20 13.4651 20C17.7959 20 21.8099 17.09 23.9436 12.4C24.0018 12.2738 24.0318 12.1377 24.0318 12C24.0318 11.8623 24.0018 11.7262 23.9436 11.6ZM13.4651 18C10.1061 18 6.94774 15.71 5.12035 12C6.94774 8.29 10.1061 6 13.4651 6C16.8241 6 19.9825 8.29 21.8099 12C19.9825 15.71 16.8241 18 13.4651 18ZM13.4651 8C12.6294 8 11.8125 8.2346 11.1177 8.67412C10.4229 9.11365 9.88133 9.73836 9.56153 10.4693C9.24174 11.2002 9.15807 12.0044 9.3211 12.7804C9.48413 13.5563 9.88654 14.269 10.4774 14.8284C11.0683 15.3878 11.8212 15.7688 12.6408 15.9231C13.4604 16.0775 14.31 15.9983 15.082 15.6955C15.8541 15.3928 16.514 14.8801 16.9782 14.2223C17.4425 13.5645 17.6903 12.7911 17.6903 12C17.6903 10.9391 17.2451 9.92172 16.4528 9.17157C15.6604 8.42143 14.5857 8 13.4651 8ZM13.4651 14C13.0473 14 12.6388 13.8827 12.2914 13.6629C11.944 13.4432 11.6732 13.1308 11.5133 12.7654C11.3534 12.3999 11.3116 11.9978 11.3931 11.6098C11.4746 11.2219 11.6758 10.8655 11.9713 10.5858C12.2667 10.3061 12.6432 10.1156 13.053 10.0384C13.4628 9.96126 13.8875 10.0009 14.2736 10.1522C14.6596 10.3036 14.9895 10.56 15.2217 10.8889C15.4538 11.2178 15.5777 11.6044 15.5777 12C15.5777 12.5304 15.3551 13.0391 14.9589 13.4142C14.5627 13.7893 14.0254 14 13.4651 14Z'
								fill='black'
							/>
						</svg>
					</button> */}

          {item?.gdcStatus === "SUBMITTED" ? null : (
            <>
              <Dropdown
                onOpenChange={() => handleGradListObj(item)}
                overlayStyle={{
                  width: "250px",
                }}
                trigger={"click"}
                menu={{ items: settingItems }}
                placement="bottom"
              >
                <button>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="24"
                    height="24"
                    viewBox="0 0 24 24"
                    fill="none"
                  >
                    <path
                      d="M12 7C12.3956 7 12.7822 6.8827 13.1111 6.66294C13.44 6.44318 13.6964 6.13082 13.8478 5.76537C13.9991 5.39992 14.0387 4.99778 13.9616 4.60982C13.8844 4.22186 13.6939 3.86549 13.4142 3.58579C13.1345 3.30608 12.7781 3.1156 12.3902 3.03843C12.0022 2.96126 11.6001 3.00087 11.2346 3.15224C10.8692 3.30362 10.5568 3.55996 10.3371 3.88886C10.1173 4.21776 10 4.60444 10 5C10 5.53043 10.2107 6.03914 10.5858 6.41421C10.9609 6.78929 11.4696 7 12 7ZM12 17C11.6044 17 11.2178 17.1173 10.8889 17.3371C10.56 17.5568 10.3036 17.8692 10.1522 18.2346C10.0009 18.6001 9.96126 19.0022 10.0384 19.3902C10.1156 19.7781 10.3061 20.1345 10.5858 20.4142C10.8655 20.6939 11.2219 20.8844 11.6098 20.9616C11.9978 21.0387 12.3999 20.9991 12.7654 20.8478C13.1308 20.6964 13.4432 20.44 13.6629 20.1111C13.8827 19.7822 14 19.3956 14 19C14 18.4696 13.7893 17.9609 13.4142 17.5858C13.0391 17.2107 12.5304 17 12 17ZM12 10C11.6044 10 11.2178 10.1173 10.8889 10.3371C10.56 10.5568 10.3036 10.8692 10.1522 11.2346C10.0009 11.6001 9.96126 12.0022 10.0384 12.3902C10.1156 12.7781 10.3061 13.1345 10.5858 13.4142C10.8655 13.6939 11.2219 13.8844 11.6098 13.9616C11.9978 14.0387 12.3999 13.9991 12.7654 13.8478C13.1308 13.6964 13.4432 13.44 13.6629 13.1111C13.8827 12.7822 14 12.3956 14 12C14 11.4696 13.7893 10.9609 13.4142 10.5858C13.0391 10.2107 12.5304 10 12 10Z"
                      fill="black"
                    />
                  </svg>
                </button>
              </Dropdown>
            </>
          )}
        </>
      ),
    },
  ];

  const onSelectChange = (newSelectedRowKeys) => {
    setSelectedRowKeys(newSelectedRowKeys);
    newSelectedRowKeys.length > 0
      ? sethasSelected(true)
      : sethasSelected(false);
  };

  const tableColumns = columns.map((item) => ({
    ...item,
  }));
 
 
 
  async function handlefetchContactsData() { 
    await dispatch(fetchContacts(folderObj?.groupId));
  }

  useEffect(() => {
    handlefetchContactsData();
  }, [folderObj]);

  useEffect(() => {
    handleFetchData();
  }, [folderObj]);

  useEffect(() => {}, [contactsHeaders]);
  useEffect(() => {}, [contacts]);

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader 
        title="Contacts"
        subtitle="Manage contacts within your institution"
        back={true}
        handleGoBack={handleGoBack}
      />

      <div className="dash-inner-page mt-[51px] max-w-full w-full overflow-x-auto  lg:px-10 px-3 h-full">
        <div className="flex items-center justify-between w-full">
          <div>
            
            <h3 className="dash-title font-[700] text-[24px] text-black1">
              {folderObj?.groupName}
            </h3>
            
          </div>

          {hasSelected ? (
            <>
              <button
                onClick={handleSubmitToNea}
                className="bg-darkBlue w-[211px] p-2 text-white font-medium text-[18px] rounded-[56px]"
              >
                Submit to NEA
              </button>
            </>
          ) : (
            <>
               
            </>
          )}
        </div>

        {contacts && contacts?.length > 0 ? ( 
          <ContactsListView handleGraduateOption={handleGraduateOption} />
        ) : (
          <div
            className="mt-[31px] bg-white w-[912px] h-[471.33px] rounded-[15px] border border-solid
           border-[#F5F7FF] py-[60px] flex flex-col justify-center items-center cursor-pointer"
            onClick={handleGraduateOption}
          >
            <div>
              <img
                src={container}
                alt="group-pic"
                className="object-contain h-[291.33px] w-[292px]"
              /> 
            </div>
            <div className="paragraph">Add contacts to this group</div>

            <div className="mt-[10px]">
              <button
                className="w-[250px] h-[50px] px-[24px] py-3 bg-blueDark rounded-[28px] justify-center items-center gap-1 inline-flex
            text-white text-[18px] leading-normal"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="25"
                  height="24"
                  viewBox="0 0 25 24"
                  fill="none"
                >
                  <path
                    d="M19.5 11H13.5V5C13.5 4.73478 13.3946 4.48043 13.2071 4.29289C13.0196 4.10536 12.7652 4 12.5 4C12.2348 4 11.9804 4.10536 11.7929 4.29289C11.6054 4.48043 11.5 4.73478 11.5 5V11H5.5C5.23478 11 4.98043 11.1054 4.79289 11.2929C4.60536 11.4804 4.5 11.7348 4.5 12C4.5 12.2652 4.60536 12.5196 4.79289 12.7071C4.98043 12.8946 5.23478 13 5.5 13H11.5V19C11.5 19.2652 11.6054 19.5196 11.7929 19.7071C11.9804 19.8946 12.2348 20 12.5 20C12.7652 20 13.0196 19.8946 13.2071 19.7071C13.3946 19.5196 13.5 19.2652 13.5 19V13H19.5C19.7652 13 20.0196 12.8946 20.2071 12.7071C20.3946 12.5196 20.5 12.2652 20.5 12C20.5 11.7348 20.3946 11.4804 20.2071 11.2929C20.0196 11.1054 19.7652 11 19.5 11Z"
                    fill="#EDF8FF"
                  />
                </svg>
                Add contact
              </button>
            </div>
          </div>
        )}
      </div>
      <AddContactListModal
        open={open}
        handleFetchData={handleFetchData}
        handleCancel={handleCancel}
      />
      <HowToAddContactModal
        open={openGraduate}
        handleCancel={handleCancelGraduate}
        uploadGraduateFile={handleUploadFile}
      />
    </div>
  );
}
