import React, { useEffect } from 'react';
import { createRoot } from 'react-dom/client';

const Rating = () => {
	const ratingBody = document.querySelectorAll('#rating > span');

	let nodeListArray = Array.from(ratingBody);

	var result = [];

	nodeListArray?.map((item) => {
		item.addEventListener('click', () => {
			item.classList.toggle('active');
			while ((item = item.previousElementSibling)) result.push(item);
		});
	});

 

	useEffect(() => {});

	return (
		<div
			className='flex items-center mt-[20%] justify-evenly w-[50%]'
			id='rating'
		>
			<span className='text-4xl cursor-pointer'>*</span>
			<span className='text-4xl cursor-pointer'>*</span>
			<span className='text-4xl cursor-pointer'>*</span>
			<span className='text-4xl cursor-pointer'>*</span>
			<span className='text-4xl cursor-pointer'>*</span>
		</div>
	);
};

export default Rating;
