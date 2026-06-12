import noCon from "../../../assets/img/noCon.png";

export default function NoCont({ handleAddFolder }) {
  function handleAdd() {
    handleAddFolder();
  }

  return (
    <>
      <div className="product_request_title !text-[31px]">
        Let’s start by creating a contact group
      </div>

      <div
        className="mt-[31px] bg-white w-[912px] h-[471.33px] rounded-[15px] border border-solid
				 border-[#cccccc] py-[60px] flex flex-col justify-center items-center cursor-pointer"
        onClick={handleAdd}
      >
        <div>
          <img
            src={noCon}
            alt="group-pic"
            className="object-contain h-[291.33px] w-[292px]"
          />
          <div className="paragraph">Create a group to organise your list of contact</div>
        </div>

        <div className="mt-[0px]">
          <button
            className="w-[270px] h-[50px] px-[24px] py-3 bg-blueDark rounded-[28px] justify-center items-center gap-1 inline-flex
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
            Create contact group
          </button>
        </div>
      </div> 
    </>
  );
}
