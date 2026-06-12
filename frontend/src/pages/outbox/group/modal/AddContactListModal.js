import React, { useRef } from 'react';
import { Form, Input, Modal, Select, Spin, Tag } from 'antd'; 
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom'; 
import {
	saveGraduateHeader,
	setGraduateHeaderObj,
	setGraduateListObj,
} from '../../../../features/contact/ContactSlice';
import toast from 'react-hot-toast';

const AddContactListModal = ({ open, handleCancel, handleFetchData }) => {
	const [form] = Form.useForm();
	const formRef = useRef(null);
	const dispatch = useDispatch();
	const navigate = useNavigate();

	const { folderObj } = useSelector((state) => state.folder);
	const { gradLoading } = useSelector((state) => state.contact);
	const { user } = useSelector((state) => state.auth);

	const onFinish = async (values) => {
		if (!folderObj?.fldId) {
			toast.error("A folder must be selected before creating a list")
			await navigate(`/group`);
		}
		values.gdcInstId = user?.usrInstId;
		values.gdcCreatedBy = user?.usrId;
		values.gdcFldId = folderObj?.fldId;

		const res = await dispatch(saveGraduateHeader(values));
		if (res?.payload?.success) {
			toast.success("List of contacts created")
			await form.resetFields();
			await handleFetchData();
			await dispatch(setGraduateHeaderObj(res?.payload?.jsonData));
			await dispatch(setGraduateListObj(res?.payload?.jsonData))
			await navigate(`/contacts/folders/list/${res?.payload?.jsonData?.gdcTitle}`)
		} else {
			toast.error(res?.payload?.messages?.message)
		}
	};

	return (
		<>
			<Modal
				className='modal-btn mod-height mod-footer'
				title='Add folder'
				open={open}
				onOk={onFinish}
				onCancel={handleCancel}
				width={850}>
				<div className='text-black3 text-[24px] mb-5'>
					Enter name of Graduate List
				</div>
				<Form
					layout='vertical'
					ref={formRef}
					name='control-ref'
					onFinish={onFinish}
					className=' '
					style={{
						maxWidth: '100%',
					}}
					form={form}>
					<Form.Item
						name='gdcTitle'
						label='Name of List (e.g Cohort-26)'
						rules={[
							{
								required: true,
								message: 'Name of list is required',
							},
						]}>
						<Input className='input' />
					</Form.Item>
					<div className='flex justify-between mt-10'>
						<div className='justify-start'></div>
						<div className='justify-end flex items-center'>
						<div className="w-[150px] ">
                <button
                  key="back"
				  onClick={handleCancel}
                  className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
                >
                  Cancel
                </button>
              </div>
							 

							<button
								key='submit'
								type='submit'
								className='w-[200px] p-2 bg-darkBlue rounded-[36px] justify-center items-center text-white text-[18px]'
								disabled={gradLoading}>
								{gradLoading ? <Spin /> : 'Save'}
							</button>
						</div>
					</div>
				</Form>
			</Modal>
		</>
	);
};
export default AddContactListModal;
