import React, { useRef, useState } from 'react';
import { Form, Input, Spin } from 'antd';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';  

export default function PasswordResetSuccess() {
	const [form] = Form.useForm();
	const formRef = useRef(null);

	const dispatch = useDispatch();
	const navigate = useNavigate();

	const { authLoading, user } = useSelector((state) => state.auth); 

	const onFinish = async () => {
		await navigate('/login'); 
	};

	return (
		<>
		 
			<div className='w-full min-w-[24rem] h-full flex flex-col justify-center items-center'>
				<div
					aria-disabled={authLoading}
					className='flex flex-col items-center mt-[7.12rem]'>
					<div className='flex flex-col items-center w-full'>
						<h3 className='heading_1'>Password reset</h3>
						<span className='zambia_login_text mt-[.75rem]'>
							Your password has been reset successfully
						</span>
					</div>
				</div>

				<div
					style={{
						maxWidth: '100%',
						width: '24rem',
						marginTop: '3.75rem',
					}}>
					<button
                        onClick={onFinish}
						disabled={authLoading}
						className='cstm-btn mt-[2.75rem]'
						type='button'>
						{authLoading ? <Spin /> : 'Back to Login'}
					</button>
				</div>
			</div>
		</>
	);
}
