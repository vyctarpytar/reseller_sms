import React from "react";
import svg6 from "../../../assets/svg/svg6.svg";
import InsideHeader from "../../../components/InsideHeader";
import AccordionPayment from "../../../components/AccordionPayment";

function PaymentMethod() {
    const prodItems = [
        {
          id:"MPESA",
          title: "M-Pesa", 
          image:svg6
        }, 
        {
            id:"MPESA",
            title: "M-Pesa", 
            image:svg6
          }, 
          {
            id:"MPESA",
            title: "M-Pesa", 
            image:svg6
          }, 
          {
            id:"MPESA",
            title: "M-Pesa", 
            image:svg6
          }, 
      ];
  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="Billing - Payment Methods"
        subtitle="Manage your payment methods here"
        back={false}
      />
      <div className="lg:px-10 px-3 flex flex-col mt-10">
        <div className="page-navigation_title">Billing - Payment Methods</div>
        <div className="paragraph mt-[1.875rem] lg:w-[77.5rem] w-full">
          You can make a payment by using M-Pesa, PayPal, DPO Pay or by sending
          a wire transfer to our bank account. Payments made online to PayPal,
          DPO Pay or sent to our M-Pesa paybill will be automatically credited
          to your account and available for use.
        </div>

        <div className="mt-16 flex">
            <AccordionPayment/>
          {/* <div className="payment-methods   lg:w-[44.66667%] w-full  items-start">
            <div className="p-5 flex justify-between payment-method">
              <div className="">M-Pesa</div>
              <div className="flex justify-end items-center">wdwd
                <img src={svg6} alt="svg6"/>
              </div>
            </div>
          </div> */}
        </div>
      </div>
    </div>
  );
}

export default PaymentMethod;
