import { useNavigate } from "react-router-dom";

export default function EmailVerified() {
    const navigate = useNavigate()

    function handleFinish(){
        navigate("/account-settings")
    }

	return (
		<>
			<div className='w-full h-full flex justify-center items-center'>
				<div className='verification_complete'>
					<svg
						xmlns='http://www.w3.org/2000/svg'
						width='32'
						height='32'
						viewBox='0 0 32 32'
						fill='none'>
						<path
							d='M11 17L14 20L21 13'
							stroke='#388E3C'
							stroke-width='2'
							stroke-linecap='round'
							stroke-linejoin='round'
						/>
						<path
							d='M16 28C22.6274 28 28 22.6274 28 16C28 9.37258 22.6274 4 16 4C9.37258 4 4 9.37258 4 16C4 22.6274 9.37258 28 16 28Z'
							stroke='#388E3C'
							stroke-width='2'
							stroke-linecap='round'
							stroke-linejoin='round'
						/>
					</svg>
					<span className='label_1 !text-[#616161] mt-[.75rem]'>
					Email address updated
					</span>
					<span className='paragraph_2 !text-[#616161] mt-[.75rem]'>
					You have successfully changed your email
					</span>

                    <div className='mt-[2.5rem] flex items-center justify-center gap-x-3'>
					<div className='w-[185px]'>
						<button onClick={handleFinish} type='button' className={`cstm-btn`}>
							Finish
						</button>
					</div>
				</div>
				</div>

				
			</div>
		</>
	);
}
