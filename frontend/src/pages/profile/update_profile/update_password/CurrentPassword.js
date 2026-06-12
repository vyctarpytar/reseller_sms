import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { Form, Input } from 'antd';

export default function CurrentPassword() {
	const dispatch = useDispatch();
	const navigate = useNavigate();

	const [form] = Form.useForm();
	const formRef = useRef(null);

	const { user, authLoading } = useSelector((state) => state.auth);


	const handleBack = () => {
		navigate('/account-settings');
	};

	const onFinishOtp = async (data) => {
		navigate('/account-settings/verify-current-password');
	};


	useEffect(() => {}, [user]);

	return (
		<>
			<div className='w-full h-full flex justify-center items-center'>
				<div aria-disabled={authLoading} className='flex flex-col items-center'>
					<div className='flex flex-col items-center w-full'>
						<h3 className='heading_1'>Current Password</h3>
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
						<div className='grid grid-cols-1 w-full'>
							<Form.Item
								name='otp'
								label={<span>Enter your current password</span>}
								className='login-form-item '
								rules={[
									{
										required: true,
										message: 'Please enter otp',
									},
								]}>
								<Input.Password className='input-login' />
							</Form.Item>

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
