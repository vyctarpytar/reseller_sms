import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { Form, Input } from 'antd';
import ReactPasswordChecklist from 'react-password-checklist';

export default function NewLoggedInPassword() {
	const dispatch = useDispatch();
	const navigate = useNavigate();

	const [form] = Form.useForm();
	const formRef = useRef(null);

	const { user, authLoading } = useSelector((state) => state.auth);

	const [password, setPassword] = useState('');

	const handleBack = () => {
		navigate('/account-settings');
	};

	const onFinish = async (data) => {
		navigate('/account-settings/password-update-complete');
	};

	useEffect(() => {}, [user]);

	return (
		<>
			<div className='w-full h-full flex justify-center items-center'>
				<div aria-disabled={authLoading} className='flex flex-col items-center'>
					<div className='flex flex-col items-center w-full'>
						<h3 className='heading_1'>New Password</h3>
					</div>

					<Form
						layout='vertical'
						ref={formRef}
						name='control-ref'
						onFinish={onFinish}
						style={{
							maxWidth: '100%',
						}}
						className='mt-[78px]'
						form={form}>
						<div className='grid grid-cols-1'>
							<Form.Item
								name='usrEmail'
								label='Enter your new password'
								extra={
									'To protect your account, we require that your password has;'
								}
								rules={[
									{
										required: true,
										message: 'Add new phone number',
									},
								]}>
								<Input.Password
									onChange={(e) => setPassword(e.target.value)}
									className='input-login'
								/>
							</Form.Item>
						</div>

						<div className='mt-[.32rem]'>
							<ReactPasswordChecklist
								className='text-darkBlue'
								rules={['minLength', 'specialChar', 'number', 'capital']}
								minLength={8}
								value={password}
								messages={{
									minLength: 'A minimum of 8 characters',
									number: 'At least one number (0-9)',
									capital: 'At least one upper case letter (A-Z)',
									// match: 'Confirm password to match with the password',
									specialChar:
										'At least one special character e.g !@#$%^&*()_+{}[]:;?/|\\',
								}}
								iconComponents={{
									ValidIcon: (
										<svg
											xmlns='http://www.w3.org/2000/svg'
											fill='none'
											viewBox='0 0 24 24'
											strokeWidth='1.5'
											stroke='currentColor'
											className='w-6 h-6 text-darkBlue mr-[.5rem]'>
											<path
												strokeLinecap='round'
												strokeLinejoin='round'
												d='M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
											/>
										</svg>
									),
									InvalidIcon: (
										<svg
											className='mr-[.5rem]'
											width='17'
											height='16'
											viewBox='0 0 17 16'
											fill='none'
											xmlns='http://www.w3.org/2000/svg'>
											<rect
												x='0.75'
												y='0.5'
												width='15'
												height='15'
												rx='3.5'
												fill='white'
											/>
											<rect
												x='0.75'
												y='0.5'
												width='15'
												height='15'
												rx='3.5'
												stroke='#388E3C'
											/>
										</svg>
									),
								}}
								validColor='#147CBC'
								iconSize={2}
							/>
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
