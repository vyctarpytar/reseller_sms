import React from 'react';
import { Breadcrumb } from 'antd';

export default function BreadCrumb({ breadList }) {
	return (
		<>
			<div className='bg-white p-4 mb-2'>
				<Breadcrumb separator='>' items={breadList} />
			</div>
		</>
	);
}
