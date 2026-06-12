import { Outlet } from 'react-router-dom';
import { Suspense, useEffect } from 'react';
import Header from './Header';
import { Puff } from 'react-loader-spinner';
import { useSelector } from 'react-redux';

export default function Layout() {
	const { isLoggedIn } = useSelector((state) => state.auth);

	useEffect(() => {}, [isLoggedIn]);

	if (isLoggedIn)
		return (
			<>
				<Header />
				<Suspense
					fallback={
						<Puff
							visible={true}
							height='80'
							width='80'
							color='#4fa94d'
							ariaLabel='puff-loading'
							wrapperStyle={{}}
							wrapperClass=''
						/>
					}>
					<Outlet />
				</Suspense>
			</>
		);

	return (
		<>
			<Outlet />
		</>
	);
}
