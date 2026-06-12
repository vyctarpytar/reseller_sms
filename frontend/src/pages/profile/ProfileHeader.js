import { NavLink } from 'react-router-dom';

export default function ProfileHeader() {
	return (
		<>
			<div className='flex items-center p-[2.5rem] bg-[#F9FAFC] w-full'>
			<NavLink
					to='/account-settings'
					className='profile_header mr-[3.19rem]'>
					Account Settings
				</NavLink>
				<NavLink to='/profile' className='profile_header mr-[3.19rem]'>
					My Profile
				</NavLink> 
			</div>
		</>
	);
}
