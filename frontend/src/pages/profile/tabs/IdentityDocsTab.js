import { Tabs } from 'antd';
import image4 from '../../../assets/img/image4.png';

export default function IdentityDocsTab() {
	const tabItems = [
		{
			label: 'NRC',
			key: '1',
			children: (
				<>
					<div className='mt-[2.5rem] p-[2.5rem] border border-[#ECECEC] rounded-[0.5rem]'>
						<img
							className='w-[20rem] h-[15rem] object-none'
							src={image4}
							alt='NRC doc'
						/>
					</div>
				</>
			),
		},
		{
			label: 'Driving License',
			key: '2',
			children: (
				<>
					<div className='mt-[2.5rem] p-[2.5rem] border border-[#ECECEC] rounded-[0.5rem]'>
						<img
							className='w-[20rem] h-[15rem] object-none'
							src={image4}
							alt='NRC doc'
						/>
					</div>
				</>
			),
		},
	];

	return (
		<>
			<span className='heading_3'>Identity Documents</span>
			<Tabs
				className='underlined_tab mt-[2.25rem]'
				defaultActiveKey='1'
				items={tabItems}
			/>
			<div className='note mt-[1.5rem]'>
				<span className='heading_5'>Note</span>
				<span className='paragraph_2'>
					This is a system generated replica of the original document and is
					only for illustrative purposes. You may not use this in any official
					capacity as a substitute to the original document
				</span>
			</div>
		</>
	);
}
