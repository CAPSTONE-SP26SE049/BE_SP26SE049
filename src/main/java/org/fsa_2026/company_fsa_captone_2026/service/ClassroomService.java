package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ClassroomMemberResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ClassroomResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ClassroomMember;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ClassroomMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomService {

        private final ClassroomMemberRepository classroomMemberRepository;
        private final AccountRepository accountRepository;

        @Transactional(readOnly = true)
        public List<ClassroomResponse> getMyClassrooms(String email) {
                Account account = accountRepository.findByEmail(email)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

                return classroomMemberRepository.findByStudentId(account.getId())
                                .stream()
                                .map(ClassroomMember::getClassroom)
                                .map(ClassroomResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ClassroomMemberResponse> getClassroomMembers(String email, String classroomId) {
                // Authenticate the request implicitly by ensuring the user exists (or we could
                // enforce they belong to the room)
                accountRepository.findByEmail(email)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

                return classroomMemberRepository.findByClassroomId(UUID.fromString(classroomId))
                                .stream()
                                .map(ClassroomMemberResponse::fromEntity)
                                .collect(Collectors.toList());
        }
}
