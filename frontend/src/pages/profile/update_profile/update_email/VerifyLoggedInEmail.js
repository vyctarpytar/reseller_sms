import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { Form, Input } from 'antd';
import { addSpaces, obfuscateEmail } from '../../../../utils';

export default function VerifyLoggedInEmail() {
	const dispatch = useDispatch();
	const navigate = useNavigate();

	const [form] = Form.useForm();
	const formRef = useRef(null);

	const { user, authLoading } = useSelector((state) => state.auth);

	const [counterOtp, setCounterOtp] = useState({ minutes: 0, seconds: 59 });

	const handleBack = () => {
		navigate('/account-settings');
	};

	const onFinishOtp = async (data) => {
		navigate('/account-settings/new-email');
	};

	const resendOTP = async () => {
		setCounterOtp({ minutes: 0, seconds: 59 });
		// const res = await dispatch(
		//   saveOtp({
		//     url: `api/v1/public/resend-email-otp/${singleUser?.drftId}`,
		//   })
		// );
		// if(res?.payload?.success){
		//   toast.success(res.payload?.messages?.message);
		// }else{
		//   toast.error(res.payload?.messages?.message);
		// }
	};

	useEffect(() => {}, [user]);

	useEffect(() => {
		const interval = setInterval(() => {
			if (counterOtp.minutes === 0 && counterOtp.seconds === 0) {
				clearInterval(interval);
			} else {
				setCounterOtp((prevCounter) => {
					if (prevCounter.seconds === 0) {
						return { minutes: prevCounter.minutes - 1, seconds: 59 };
					} else {
						return { ...prevCounter, seconds: prevCounter.seconds - 1 };
					}
				});
			}
		}, 1000);

		return () => clearInterval(interval);
	}, [counterOtp]);

	return (
		<>
			<div className='w-full h-full flex justify-center items-center'>
				<div aria-disabled={authLoading} className='flex flex-col items-center'>
					<div className='flex flex-col items-center w-full'>
						<h3 className='heading_1'>Verification</h3>
					</div>

					<Form
						layout='vertical'
						ref={formRef}
						name='control-ref'
						onFinish={onFinishOtp}
						style={{
							maxWidth: '100%',
						}}
						className='mt-[78px]'
						form={form}>
						<div className='grid grid-cols-1'>
							<Form.Item
								name='otp'
								label={<span>Enter OTP sent to {user?.drftEmail ? obfuscateEmail(user?.drftEmail) : null}</span>}
								className='login-form-item '
								rules={[
									{
										required: true,
										message: 'Please enter otp',
									},
                                    {
                                        min: 5,
                                    },
                                    {
                                        max: 7,
                                    },
                                    {
                                        pattern: /^(?=.*\d)(?!.*[a-z]).*$/,
                                        message: 'Should include UPPERCASE LETTERS and atleast one number',
                                    }
								]}>
								<Input className='input-login' />
							</Form.Item>
							<div className='mt-[.2rem] flex'>
								{counterOtp.minutes === 0 && counterOtp.seconds === 0 ? (
									<button
										type='button'
										className='resend_otp_text'
										onClick={resendOTP}
										disabled={counterOtp.minutes > 0 && counterOtp.seconds > 0}>
										Didn’t receive OTP?{' '}
										<span className='resend_otp_text !text-[#0057E3]'>
											Resend
										</span>
									</button>
								) : (
									<span className='resend_otp_text'>
										{' '}
										Resend in {' '}
										<span className='!text-[#0057E3]'>
											{counterOtp.minutes.toString().padStart(2, '0')}:
											{counterOtp.seconds.toString().padStart(2, '0')}
										</span>
									</span>
								)}
							</div>
						</div>
						<div className='mt-[2rem] flex items-center justify-center gap-x-3'>
							<div className='w-[185px]'>
								<button
									className={`cstm-btn !bg-[#fff] !text-[#374151] border border-[#D1D5DB]`}
									onClick={handleBack}>
									Back
								</button>
							</div>

							<div className='w-[185px]'>
								<button type='submit' key='submit' className={`cstm-btn`}>
									Next
								</button>
							</div>
						</div>
					</Form>
				</div>
			</div>
		</>
	);
}
