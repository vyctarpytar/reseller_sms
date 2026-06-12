import { Tabs } from 'antd';

export default function OrganizationsTab() {
	return (
		<>
			<div className='flex flex-col w-full'>
				<span className='heading_3'>Organisations</span>
				<span className='heading_3 !text-[#6B7280] mt-[2.5rem]'>
					Businesses & Companies
				</span>
				<div className='org_card mt-[2.5rem]'>
					<span className='heading_3'>JANE’S SHOE STORE</span>

					<div className='mt-[2.69rem] flex items-center gap-x-[1.81rem]'>
						<div className='flex flex-col justify-start items-start'>
							<span className='label_2 !text-[#6B7280]'>Entity type</span>
							<span className='paragraph_1 !text-blk21 mt-[.75rem]'>
								Sole trader
							</span>
						</div>

						<div className='flex flex-col justify-start items-start'>
							<span className='label_2 !text-[#6B7280]'>Registration</span>
							<span className='paragraph_1 !text-blk21 mt-[.75rem]'>
								JAK -003
							</span>
						</div>
					</div>
				</div>
			</div>
		</>
	);
}
