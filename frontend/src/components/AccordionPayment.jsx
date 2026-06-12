import React, { useState } from "react";
import svg6 from "../assets/svg/svg6.svg";
import svg7 from "../assets/svg/svg7.svg";
import svg40 from "../assets/svg/svg40.svg";
 
import mpesa from "../assets/img/mpesa.png";

function FaqItem({ question, answer, isOpen, toggleAccordion }) {
  return (
    <div className={`payment-methods`}>
      <button
        onClick={toggleAccordion}
        className={`flex items-center justify-between w-full  `}
      >
        <h1 className={` px-3  py-[14px]`}>
          <div className="flex flex-col justify-start">
            <span className="flex justify-start payment-method ">
              {question}
            </span>
          </div>
        </h1>
        {isOpen ? (
          <div className="flex items-center gap-x-2">
            <img src={mpesa} alt="mpesa" className="h-[2rem]  object-cover" />
            <img src={svg7} alt="svg7" />
          </div>
        ) : (
          <div className="flex items-center gap-x-2">
            <img src={mpesa} alt="mpesa" className="h-[2rem]  object-cover" />
            <img src={svg6} alt="svg6" />
          </div>
        )}
      </button>

      {isOpen && (
        <> 
        <p className="mt-0 flex flex-col justify-start items-start px-3 pt-0 pb-8">
          <div className="prod_view_subtitle !text-[16px]"> Pay Bill</div>
          <ul className="mt-5">
            <li>
              Using your MPesa-enabled phone, select "Pay Bill" from the M-Pesa
              menu
            </li>
            <li> Enter SPA Business Number 525900</li>
            <li>
              Enter your Account Number. Your account number is jobMain.api
            </li>
            <li>Enter the Amount of credits you want to buy</li>
            <li>Confirm that all the details are correct and press Ok</li>
            <li>
              Check your statement to see your payment. Your API account will
              also be updated
            </li>
          </ul> 
        </p>
        <div className="flex justify-between">
          <div></div>
        <div className="w-[160px] flex justify-end mt-2 mb-3 px-4">
            <button className="cstm-btn flex items-center gap-x-1">Proceed
              <img src={svg40} alt="svg40"/>
            </button>
            </div> 
            </div>
        </>
      )}
    </div>
  );
}

function AccordionPayment() {
  const [openIndex, setOpenIndex] = useState(null);

  const toggleAccordion = (index) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  const faqData = [
    {
      question: "Mpesa",
      answer:
        "Anyone who is a Zambian citizen, resident or foreigner can access the eCitizen platform by simply registering for an account.",
    },
  ];

  return (
    <div className="mt-0 space-y-3 lg:w-[40%] w-full">
      {faqData.map((faq, index) => (
        <FaqItem
          key={index}
          question={faq.question}
          answer={faq.answer}
          isOpen={openIndex === index}
          toggleAccordion={() => toggleAccordion(index)}
        />
      ))}
    </div>
  );
}

export default AccordionPayment;
