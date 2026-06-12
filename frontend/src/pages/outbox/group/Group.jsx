import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import {
  fetchContFolders,
  folderCleanUp,
} from "../../../features/folder/folderSlice";
import AddFolderModal from "./modal/AddFolderModal";
import ContactFolderTable from "./ContactFolderTable";
import NoCont from "./NoCont";
import InsideHeader from "../../../components/InsideHeader";

export default function Group() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const { user } = useSelector((state) => state.auth);
  const { gradFolders, fldLoading } = useSelector((state) => state.folder);

  const [open, setopen] = useState(false);

  function handleCancel() {
    setopen(false);
  }

  function handleAddFolder() {
    setopen(true);
  }

  const handleGoBack = () => {
    navigate("/outbox");
  };

  async function handleFetchData() {
    await dispatch(fetchContFolders());
  }

  useEffect(() => {}, [gradFolders]);

  useEffect(() => {
    handleFetchData();
    dispatch(folderCleanUp());
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Contacts"
          subtitle="Manage contacts within your institution"
          back={true}
          handleGoBack={handleGoBack}
        />

        <div className="mt-[51px] lg:px-10 px-3 h-full">
          {gradFolders?.length ? (
            <ContactFolderTable
              handleFetchData={handleFetchData}
              handleAddFolder={handleAddFolder}
            />
          ) : (
            <NoCont handleAddFolder={handleAddFolder} />
          )}
        </div>
        <AddFolderModal
          handleFetchData={handleFetchData}
          open={open}
          handleCancel={handleCancel}
        />
      </div>
    </>
  );
}
