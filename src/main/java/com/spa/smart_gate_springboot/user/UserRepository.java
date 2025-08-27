package com.spa.smart_gate_springboot.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsrId(UUID usrId);

    @Query(value = """
                SELECT * FROM js_core.stp_user
                WHERE 1=1
              and (case when   :layer = 'TOP'  then  layer = 'TOP'  else 1=1 end )
              and (case when   cast(:accId as UUID) is not null then  usr_acc_id =  cast(:accId as UUID)  else 1=1 end )
             AND (case when cast ( :resellerId as UUID) IS not NULL then   usr_reseller_id = cast ( :resellerId as UUID)  else 1=1 end)
             AND (case when :status IS not  NULL then  usr_status = :status else 1=1 end )
             AND (case when :firstname IS not  NULL then  firstname ilike :firstname else 1=1 end )
             AND (case when :lastname IS not  NULL then  lastname ilike :lastname else 1=1 end )
             AND (case when :phoneNumber IS not  NULL then  phone_number ilike :phoneNumber else 1=1 end )
             AND (case when :email IS not  NULL then  email ilike :email else 1=1 end )
             AND (case when :usrNationalId IS not  NULL then  usr_national_id ilike :usrNationalId else 1=1 end )
            """, nativeQuery = true)
    Page<User> getAllUsersFiltered(
            @Param("layer") String layer,
            @Param("accId") UUID accId,
            @Param("resellerId") UUID resellerId,
            @Param("firstname") String firstname,
            @Param("lastname") String lastname,
            @Param("email") String email,
            @Param("status") String status,
            @Param("usrNationalId") String usrNationalId,
            @Param("phoneNumber") String phoneNumber,
            Pageable pageable
    );

    List<User> findAllByUsrResellerId(UUID resellerId);

    List<User> findAllByUsrAccId(UUID accId);
}
